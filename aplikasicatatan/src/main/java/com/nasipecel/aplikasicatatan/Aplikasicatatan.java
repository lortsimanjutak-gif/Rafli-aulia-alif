package com.nasipecel.aplikasicatatan;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.security.MessageDigest;
import java.sql.*;

public class Aplikasicatatan extends JFrame {

    private static final String DB =
        "jdbc:mysql://localhost:3306/aplikasi_catatan";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private Connection conn;
    private int userId;
    private String namaUser;

    private JTextField emailField, namaField;
    private JPasswordField passwordField;
    private JTextField judulField, cariField;
    private JTextArea isiArea;
    private JTable tabel;
    private DefaultTableModel model;
    private JCheckBox favoritCheck;

    public Aplikasicatatan() {
        try {
            conn = DriverManager.getConnection(DB, DB_USER, DB_PASS);
            tampilLogin();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                null,
                "Koneksi database gagal: " + e.getMessage()
            );
        }
    }

    // Mengubah password menjadi hash SHA-256
    private String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hasil = md.digest(
            password.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StringBuilder sb = new StringBuilder();

        for (byte b : hasil) {
            sb.append(String.format("%02x", b & 0xff));
        }

        return sb.toString();
    }

    // ================= LOGIN =================

    private void tampilLogin() {
        getContentPane().removeAll();

        setTitle("Login - Aplikasi Catatan");
        setSize(400, 270);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel p = new JPanel(new GridLayout(4, 2, 8, 8));
        p.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        emailField = new JTextField();
        passwordField = new JPasswordField();

        JButton loginBtn = new JButton("Login");
        JButton daftarBtn = new JButton("Registrasi");

        p.add(new JLabel("Email:"));
        p.add(emailField);

        p.add(new JLabel("Password:"));
        p.add(passwordField);

        p.add(loginBtn);
        p.add(daftarBtn);

        loginBtn.addActionListener(e -> login());
        daftarBtn.addActionListener(e -> tampilRegistrasi());
        passwordField.addActionListener(e -> login());

        add(p);

        revalidate();
        repaint();
        setVisible(true);
    }

    private void login() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Email dan password wajib diisi."
            );
            return;
        }

        String sql =
            "SELECT id, nama FROM users WHERE email=? AND password=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hashPassword(password));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getInt("id");
                    namaUser = rs.getString("nama");

                    tampilDashboard();
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Email atau password salah."
                    );
                }
            }
        } catch (Exception e) {
            pesanError(e);
        }
    }

    // ================= REGISTRASI =================

    private void tampilRegistrasi() {
        getContentPane().removeAll();

        setTitle("Registrasi Akun");
        setSize(400, 320);
        setLocationRelativeTo(null);

        JPanel p = new JPanel(new GridLayout(5, 2, 8, 8));
        p.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        namaField = new JTextField();
        emailField = new JTextField();
        passwordField = new JPasswordField();

        JButton daftarBtn = new JButton("Daftar");
        JButton kembaliBtn = new JButton("Kembali");

        p.add(new JLabel("Nama:"));
        p.add(namaField);

        p.add(new JLabel("Email:"));
        p.add(emailField);

        p.add(new JLabel("Password:"));
        p.add(passwordField);

        p.add(daftarBtn);
        p.add(kembaliBtn);

        daftarBtn.addActionListener(e -> registrasi());
        kembaliBtn.addActionListener(e -> tampilLogin());

        add(p);

        revalidate();
        repaint();
        setVisible(true);
    }

    private void registrasi() {
        String nama = namaField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (nama.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Semua kolom wajib diisi."
            );
            return;
        }

        if (password.length() < 8) {
            JOptionPane.showMessageDialog(
                this,
                "Password minimal 8 karakter."
            );
            return;
        }

        String sql =
            "INSERT INTO users(nama, email, password) VALUES(?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nama);
            ps.setString(2, email);
            ps.setString(3, hashPassword(password));

            ps.executeUpdate();

            JOptionPane.showMessageDialog(
                this,
                "Registrasi berhasil. Silakan login."
            );

            tampilLogin();

        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(
                this,
                "Email sudah terdaftar."
            );
        } catch (Exception e) {
            pesanError(e);
        }
    }

    // ================= DASHBOARD =================

    private void tampilDashboard() {
        getContentPane().removeAll();

        setTitle("Aplikasi Catatan - " + namaUser);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JPanel utama = new JPanel(new BorderLayout(10, 10));
        utama.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        JPanel atas = new JPanel(new BorderLayout());

        atas.add(
            new JLabel("Selamat datang, " + namaUser),
            BorderLayout.WEST
        );

        JButton logoutBtn = new JButton("Logout");
        atas.add(logoutBtn, BorderLayout.EAST);

        logoutBtn.addActionListener(e -> logout());

        model = new DefaultTableModel(
            new String[]{"ID", "Judul", "Isi", "Favorit"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabel = new JTable(model);
        tabel.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        tabel.getColumnModel().getColumn(0).setPreferredWidth(40);
        tabel.getColumnModel().getColumn(1).setPreferredWidth(180);
        tabel.getColumnModel().getColumn(2).setPreferredWidth(400);

        JScrollPane scroll = new JScrollPane(tabel);

        JPanel form = new JPanel(new BorderLayout(5, 5));
        JPanel inputAtas = new JPanel(new BorderLayout(5, 5));

        judulField = new JTextField();

        inputAtas.add(new JLabel("Judul:"), BorderLayout.WEST);
        inputAtas.add(judulField, BorderLayout.CENTER);

        isiArea = new JTextArea(4, 30);
        isiArea.setLineWrap(true);
        isiArea.setWrapStyleWord(true);

        favoritCheck = new JCheckBox("Favorit");

        JPanel editor = new JPanel(new BorderLayout(5, 5));

        editor.add(inputAtas, BorderLayout.NORTH);
        editor.add(new JScrollPane(isiArea), BorderLayout.CENTER);
        editor.add(favoritCheck, BorderLayout.SOUTH);

        JButton tambahBtn = new JButton("Tambah");
        JButton lihatBtn = new JButton("Lihat");
        JButton editBtn = new JButton("Perbarui");
        JButton hapusBtn = new JButton("Hapus");
        JButton favoritBtn = new JButton("Daftar Favorit");
        JButton semuaBtn = new JButton("Semua Catatan");
        JButton cariBtn = new JButton("Cari");

        JPanel tombol = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );

        tombol.add(tambahBtn);
        tombol.add(lihatBtn);
        tombol.add(editBtn);
        tombol.add(hapusBtn);
        tombol.add(favoritBtn);
        tombol.add(semuaBtn);

        JPanel pencarian = new JPanel(new BorderLayout(5, 5));

        cariField = new JTextField();

        pencarian.add(cariField, BorderLayout.CENTER);
        pencarian.add(cariBtn, BorderLayout.EAST);

        JPanel bawah = new JPanel(new BorderLayout(5, 5));

        bawah.add(editor, BorderLayout.CENTER);
        bawah.add(tombol, BorderLayout.SOUTH);

        utama.add(atas, BorderLayout.NORTH);
        utama.add(scroll, BorderLayout.CENTER);

        JPanel bagianBawah = new JPanel(
            new BorderLayout(5, 5)
        );

        bagianBawah.add(pencarian, BorderLayout.NORTH);
        bagianBawah.add(bawah, BorderLayout.CENTER);

        utama.add(bagianBawah, BorderLayout.SOUTH);

        tambahBtn.addActionListener(e -> tambahCatatan());
        lihatBtn.addActionListener(e -> lihatCatatan());
        editBtn.addActionListener(e -> editCatatan());
        hapusBtn.addActionListener(e -> hapusCatatan());
        cariBtn.addActionListener(e -> cariCatatan());

        favoritBtn.addActionListener(e -> muatCatatan(true));
        semuaBtn.addActionListener(e -> muatCatatan(false));

        tabel.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()
                    && tabel.getSelectedRow() >= 0) {

                int row = tabel.getSelectedRow();

                judulField.setText(
                    model.getValueAt(row, 1).toString()
                );

                isiArea.setText(
                    model.getValueAt(row, 2).toString()
                );

                favoritCheck.setSelected(
                    Boolean.parseBoolean(
                        model.getValueAt(row, 3).toString()
                    )
                );
            }
        });

        add(utama);

        revalidate();
        repaint();

        muatCatatan(false);
    }

    private int idTerpilih() {
        int row = tabel.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(
                this,
                "Pilih catatan terlebih dahulu."
            );
            return -1;
        }

        return Integer.parseInt(
            model.getValueAt(row, 0).toString()
        );
    }

    // ================= TAMBAH CATATAN =================

    private void tambahCatatan() {
        String judul = judulField.getText().trim();
        String isi = isiArea.getText().trim();

        if (judul.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Judul wajib diisi."
            );
            return;
        }

        String sql =
            "INSERT INTO catatan(user_id, judul, isi, favorit) "
            + "VALUES(?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, judul);
            ps.setString(3, isi);
            ps.setBoolean(4, favoritCheck.isSelected());

            ps.executeUpdate();

            bersihkanForm();
            muatCatatan(false);

            JOptionPane.showMessageDialog(
                this,
                "Catatan berhasil ditambahkan."
            );

        } catch (SQLException e) {
            pesanError(e);
        }
    }

    // ================= LIHAT CATATAN =================

    private void lihatCatatan() {
        int id = idTerpilih();

        if (id == -1) {
            return;
        }

        JOptionPane.showMessageDialog(
            this,
            "Judul: " + judulField.getText()
                + "\n\nIsi:\n" + isiArea.getText(),
            "Detail Catatan",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ================= EDIT CATATAN =================

    private void editCatatan() {
        int id = idTerpilih();

        if (id == -1) {
            return;
        }

        String judul = judulField.getText().trim();
        String isi = isiArea.getText().trim();

        if (judul.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Judul wajib diisi."
            );
            return;
        }

        String sql =
            "UPDATE catatan SET judul=?, isi=?, favorit=? "
            + "WHERE id=? AND user_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, judul);
            ps.setString(2, isi);
            ps.setBoolean(3, favoritCheck.isSelected());
            ps.setInt(4, id);
            ps.setInt(5, userId);

            ps.executeUpdate();

            bersihkanForm();
            muatCatatan(false);

            JOptionPane.showMessageDialog(
                this,
                "Catatan berhasil diperbarui."
            );

        } catch (SQLException e) {
            pesanError(e);
        }
    }

    // ================= HAPUS CATATAN =================

    private void hapusCatatan() {
        int id = idTerpilih();

        if (id == -1) {
            return;
        }

        int pilihan = JOptionPane.showConfirmDialog(
            this,
            "Yakin ingin menghapus catatan ini?",
            "Konfirmasi Hapus",
            JOptionPane.YES_NO_OPTION
        );

        if (pilihan != JOptionPane.YES_OPTION) {
            return;
        }

        String sql =
            "DELETE FROM catatan WHERE id=? AND user_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);

            ps.executeUpdate();

            bersihkanForm();
            muatCatatan(false);

            JOptionPane.showMessageDialog(
                this,
                "Catatan berhasil dihapus."
            );

        } catch (SQLException e) {
            pesanError(e);
        }
    }

    // ================= PENCARIAN =================

    private void cariCatatan() {
        String kata = cariField.getText().trim();

        String sql =
            "SELECT id, judul, isi, favorit FROM catatan "
            + "WHERE user_id=? AND (judul LIKE ? OR isi LIKE ?) "
            + "ORDER BY id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, "%" + kata + "%");
            ps.setString(3, "%" + kata + "%");

            tampilHasil(ps);

        } catch (SQLException e) {
            pesanError(e);
        }
    }

    // ================= MUAT CATATAN =================

    private void muatCatatan(boolean hanyaFavorit) {
        String sql =
            "SELECT id, judul, isi, favorit FROM catatan "
            + "WHERE user_id=?";

        if (hanyaFavorit) {
            sql += " AND favorit=TRUE";
        }

        sql += " ORDER BY id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            tampilHasil(ps);

        } catch (SQLException e) {
            pesanError(e);
        }
    }

    private void tampilHasil(PreparedStatement ps)
            throws SQLException {

        model.setRowCount(0);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String isi = rs.getString("isi");

                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("judul"),
                    isi == null ? "" : isi,
                    rs.getBoolean("favorit")
                });
            }
        }
    }

    private void bersihkanForm() {
        judulField.setText("");
        isiArea.setText("");
        favoritCheck.setSelected(false);
        tabel.clearSelection();
    }

    // ================= LOGOUT =================

    private void logout() {
        int pilihan = JOptionPane.showConfirmDialog(
            this,
            "Apakah Anda yakin ingin logout?",
            "Konfirmasi Logout",
            JOptionPane.YES_NO_OPTION
        );

        if (pilihan == JOptionPane.YES_OPTION) {
            userId = 0;
            namaUser = null;

            tampilLogin();
        }
    }

    // ================= PESAN ERROR =================

    private void pesanError(Exception e) {
        JOptionPane.showMessageDialog(
            this,
            "Terjadi kesalahan: " + e.getMessage(),
            "Kesalahan",
            JOptionPane.ERROR_MESSAGE
        );
    }

    // ================= MAIN =================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Aplikasicatatan();
        });
    }
}