package com.nexus.ims.view;

import com.nexus.ims.model.User;
import com.nexus.ims.util.IconFactory;
import com.nexus.ims.util.LanguageManager;
import com.nexus.ims.util.ThemeManager;
import com.nexus.ims.util.ThemeManager.BadgeColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin user and RBAC management panel with role filtering and selection-aware toolbar actions.
 */
public class UsersPanel extends JPanel {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JTextField searchField;
    private JComboBox<String> roleFilterCombo;
    private JButton refreshBtn;
    private JButton addUserBtn;
    private JButton editUserBtn;
    private JButton resetPasswordBtn;
    private JButton toggleActiveBtn;
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JScrollPane scrollPane;

    public UsersPanel() {
        initComponents();
        refreshThemeAndLanguage();
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        // Top Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        filterBox.setOpaque(false);

        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(250, 36));
        searchField.putClientProperty("JTextField.showClearButton", true);

        roleFilterCombo = new JComboBox<>(new String[]{"ALL", "ADMIN", "MANAGER", "STAFF"});
        roleFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleFilterCombo.setPreferredSize(new Dimension(135, 36));
        roleFilterCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    String key = value.toString();
                    label.setText("ALL".equals(key)
                            ? LanguageManager.get("users.filter.all")
                            : LanguageManager.get("role." + key));
                }
                label.setHorizontalAlignment(SwingConstants.LEADING);
                return label;
            }
        });

        filterBox.add(searchField);
        filterBox.add(roleFilterCombo);
        toolbar.add(filterBox, BorderLayout.LINE_START);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.setOpaque(false);

        refreshBtn = new JButton();
        refreshBtn.setIconTextGap(6);

        addUserBtn = new JButton();
        addUserBtn.setIconTextGap(6);
        addUserBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));

        editUserBtn = new JButton();
        editUserBtn.setIconTextGap(6);

        resetPasswordBtn = new JButton();
        resetPasswordBtn.setIconTextGap(6);

        toggleActiveBtn = new JButton();
        toggleActiveBtn.setIconTextGap(6);

        actions.add(refreshBtn);
        actions.add(addUserBtn);
        actions.add(editUserBtn);
        actions.add(resetPasswordBtn);
        actions.add(toggleActiveBtn);
        toolbar.add(actions, BorderLayout.LINE_END);

        add(toolbar, BorderLayout.NORTH);

        // Users Table
        String[] columns = new String[6];
        userTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.getTableHeader().setReorderingAllowed(false);
        userTable.setDefaultRenderer(Object.class, new UserCellRenderer());
        userTable.getColumnModel().getColumn(3).setCellRenderer(new RoleBadgeRenderer());
        userTable.getColumnModel().getColumn(4).setCellRenderer(new AccountStatusBadgeRenderer());

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(userTableModel);
        sorter.setComparator(0, DashboardFrame.NUMERIC_COMPARATOR);
        userTable.setRowSorter(sorter);

        TableColumnModel cm = userTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(55);
        cm.getColumn(0).setMaxWidth(75);
        cm.getColumn(1).setPreferredWidth(160);
        cm.getColumn(2).setPreferredWidth(240);
        cm.getColumn(3).setPreferredWidth(120);
        cm.getColumn(4).setPreferredWidth(135);
        cm.getColumn(5).setPreferredWidth(170);

        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectionState();
            }
        });

        scrollPane = new JScrollPane(userTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void updateSelectionState() {
        int viewRow = userTable.getSelectedRow();
        boolean hasSelection = viewRow != -1;

        editUserBtn.setEnabled(hasSelection);
        resetPasswordBtn.setEnabled(hasSelection);
        toggleActiveBtn.setEnabled(hasSelection);

        Color secondaryFg = hasSelection ? ThemeManager.btnSecondaryFg() : ThemeManager.textMuted();
        editUserBtn.setForeground(secondaryFg);
        editUserBtn.setIcon(IconFactory.edit(15, secondaryFg));

        resetPasswordBtn.setForeground(secondaryFg);
        resetPasswordBtn.setIcon(IconFactory.key(15, secondaryFg));

        if (!hasSelection) {
            toggleActiveBtn.setText(LanguageManager.get("users.toggleStatus"));
            toggleActiveBtn.setForeground(ThemeManager.textMuted());
            toggleActiveBtn.setIcon(IconFactory.statusDot(12, ThemeManager.textMuted()));
        } else {
            int modelRow = userTable.convertRowIndexToModel(viewRow);
            String status = String.valueOf(userTableModel.getValueAt(modelRow, 4));
            boolean isActive = "ACTIVE".equals(status);
            toggleActiveBtn.setText(isActive
                    ? LanguageManager.get("users.deactivate")
                    : LanguageManager.get("users.activate"));
            Color dotColor = isActive ? ThemeManager.accentDanger() : ThemeManager.accentSuccess();
            toggleActiveBtn.setForeground(dotColor);
            toggleActiveBtn.setIcon(IconFactory.statusDot(12, dotColor));
        }
    }

    public void refreshThemeAndLanguage() {
        searchField.putClientProperty("JTextField.leadingIcon", IconFactory.search(15, ThemeManager.textSecondary()));
        searchField.putClientProperty("JTextField.placeholderText", LanguageManager.get("users.searchPlaceholder"));
        searchField.setBackground(ThemeManager.bgSurface());
        searchField.setForeground(ThemeManager.textPrimary());

        roleFilterCombo.setBackground(ThemeManager.bgSurface());
        roleFilterCombo.setForeground(ThemeManager.textPrimary());

        refreshBtn.setText(LanguageManager.get("common.refresh"));
        refreshBtn.setBackground(ThemeManager.btnSecondaryBg());
        refreshBtn.setForeground(ThemeManager.btnSecondaryFg());
        refreshBtn.setIcon(IconFactory.refresh(15, ThemeManager.btnSecondaryFg()));

        addUserBtn.setText(LanguageManager.get("users.add"));
        addUserBtn.setBackground(ThemeManager.accentPrimary());
        addUserBtn.setForeground(Color.WHITE);
        addUserBtn.setIcon(IconFactory.add(15, Color.WHITE));

        editUserBtn.setText(LanguageManager.get("users.edit"));
        editUserBtn.setBackground(ThemeManager.btnSecondaryBg());

        resetPasswordBtn.setText(LanguageManager.get("users.resetPassword"));
        resetPasswordBtn.setBackground(ThemeManager.btnSecondaryBg());

        toggleActiveBtn.setBackground(ThemeManager.btnSecondaryBg());
        updateSelectionState();

        String[] headers = {
                LanguageManager.get("common.id"),
                LanguageManager.get("users.col.username"),
                LanguageManager.get("users.col.fullName"),
                LanguageManager.get("users.col.role"),
                LanguageManager.get("users.col.status"),
                LanguageManager.get("users.col.createdAt")
        };
        TableColumnModel cm = userTable.getColumnModel();
        for (int i = 0; i < headers.length && i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setHeaderValue(headers[i]);
        }
        if (userTable.getTableHeader() != null) {
            userTable.getTableHeader().repaint();
        }

        scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, ThemeManager.borderSubtle()));
        scrollPane.getViewport().setBackground(ThemeManager.bgSurface());
        userTable.setBackground(ThemeManager.bgSurface());
        userTable.setForeground(ThemeManager.textPrimary());
        userTable.setGridColor(ThemeManager.borderSubtle());

        LanguageManager.applyOrientation(this);
        roleFilterCombo.repaint();
        userTable.repaint();
    }

    public void populateUsers(List<User> users) {
        userTableModel.setRowCount(0);
        if (users != null) {
            for (User u : users) {
                userTableModel.addRow(new Object[]{
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getRole().name(),
                        u.isActive() ? "ACTIVE" : "INACTIVE",
                        u.getCreatedAt() != null ? u.getCreatedAt().format(TS_FORMAT) : "N/A"
                });
            }
        }
        updateSelectionState();
    }

    public JTextField getSearchField() { return searchField; }
    public JComboBox<String> getRoleFilterCombo() { return roleFilterCombo; }
    public JButton getRefreshBtn() { return refreshBtn; }
    public JButton getAddUserBtn() { return addUserBtn; }
    public JButton getEditUserBtn() { return editUserBtn; }
    public JButton getResetPasswordBtn() { return resetPasswordBtn; }
    public JButton getToggleActiveBtn() { return toggleActiveBtn; }
    public JTable getUserTable() { return userTable; }
    public DefaultTableModel getUserTableModel() { return userTableModel; }

    private static class UserCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            cell.setBorder(new EmptyBorder(0, 12, 0, 12));
            cell.setHorizontalAlignment(column == 0 ? SwingConstants.TRAILING : SwingConstants.LEADING);
            if ((column == 0 || column == 5) && value != null) {
                cell.setText(LanguageManager.isolateLTR(value.toString()));
            }
            if (column == 1) {
                cell.setFont(cell.getFont().deriveFont(Font.BOLD));
            }
            if (!isSelected) {
                cell.setBackground(row % 2 == 0 ? ThemeManager.bgSurface() : ThemeManager.bgSurfaceAlt());
                cell.setForeground(ThemeManager.textPrimary());
            }
            return cell;
        }
    }

    private static class RoleBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String role = value != null ? value.toString() : "";
            BadgeColors colors;
            if ("ADMIN".equals(role)) {
                colors = ThemeManager.badgeWarning();
            } else if ("MANAGER".equals(role)) {
                colors = ThemeManager.badgePurple();
            } else {
                colors = ThemeManager.badgeInfo();
            }
            String display = role.isEmpty() ? "" : LanguageManager.get("role." + role);
            return createBadgeCell(table, isSelected, row, display, colors.background(), colors.foreground());
        }
    }

    private static class AccountStatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String status = value != null ? value.toString() : "";
            boolean active = "ACTIVE".equals(status);
            BadgeColors colors = active ? ThemeManager.badgeSuccess() : ThemeManager.badgeDanger();
            String display = active ? LanguageManager.get("status.active") : LanguageManager.get("status.disabled");
            return createBadgeCell(table, isSelected, row, display, colors.background(), colors.foreground());
        }
    }

    private static JPanel createBadgeCell(JTable table, boolean isSelected, int row, String text, Color bg, Color fg) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(isSelected ? table.getSelectionBackground()
                : (row % 2 == 0 ? ThemeManager.bgSurface() : ThemeManager.bgSurfaceAlt()));

        JLabel pill = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        pill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pill.setForeground(fg);
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(3, 10, 3, 10));
        wrapper.add(pill);
        return wrapper;
    }
}
