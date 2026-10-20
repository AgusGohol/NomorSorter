import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class NomorSorter extends JFrame {
    // Naikkan bersama berkas VERSION setiap rilis; tag GitHub memakai format v<versi>.
    static final String APP_VERSION = "2.7.0";
    private static final String UPDATE_REPO = "AgusGohol/NomorSorter";
    private static final Color BACKGROUND = new Color(10, 18, 32);
    private static final Color PANEL = new Color(18, 32, 52);
    private static final Color INPUT = new Color(12, 25, 44);
    private static final Color TEXT = new Color(238, 246, 255);
    private static final Color MUTED = new Color(157, 179, 205);
    private static final Color ACCENT = new Color(52, 211, 163);
    private static final Color BORDER = new Color(43, 65, 89);
    private final Path directory = Paths.get("data");
    private Path yesFile, noFile;
    private int activeDevice = 1;
    private String activeDate = today();
    private String lastKnownDay = today();
    private int deviceCount = 5;
    private final List<String> drafts = new ArrayList<>();
    private final List<String> deviceDates = new ArrayList<>();
    private final JPanel devicesPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 6));
    private ButtonGroup deviceGroup = new ButtonGroup();
    private boolean updatingDates;
    private final JComboBox<String> datePicker = new JComboBox<>();
    private final LinkedHashSet<String> yes = new LinkedHashSet<>(), no = new LinkedHashSet<>();
    private final LinkedHashSet<String> hiddenYes = new LinkedHashSet<>(), hiddenNo = new LinkedHashSet<>();
    private final Map<String, String> yesCom = new LinkedHashMap<>(), noCom = new LinkedHashMap<>();
    private final JTextArea input = new JTextArea(5, 42);
    private final JTextField search = new JTextField(20);
    private final JLabel count = new JLabel();
    private final JTabbedPane tabs = new JTabbedPane();
    private final DefaultTableModel[] models = new DefaultTableModel[2];
    private final JTable[] tables = new JTable[2];
    private final TableRowSorter<DefaultTableModel>[] sorters = new TableRowSorter[2];
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    @SuppressWarnings("unchecked")
    private final JComboBox<String>[] databaseDates = new JComboBox[]{new JComboBox<>(), new JComboBox<>()};
    private final DefaultTableModel[] databaseModels = new DefaultTableModel[2];
    private final JTable[] databaseTables = new JTable[2];
    private final JTextField[] databaseSearch = {new JTextField(19), new JTextField(19)};
    private final JLabel[] databaseInfo = {new JLabel(), new JLabel()};
    private boolean updatingDatabaseDates;
    private static final Pattern PHONE = Pattern.compile("(?<![0-9])(?:\\+?62|0)?8[0-9]{8,12}(?![0-9])");
    private static final Pattern WA_TAG = Pattern.compile("(?i)(?<![\\p{L}\\p{N}])wa(?![\\p{L}\\p{N}])");
    private static final Pattern NO_WA_TAG = Pattern.compile("(?i)\\b(?:belum|tidak|non|no)\\s*[-_ ]*wa\\b");
    private static final Pattern COM_TAG = Pattern.compile("(?i)(?<![a-z0-9])COM\\s*[-:]?\\s*(\\d+)(?!\\d)");

    @SuppressWarnings("unchecked")
    public NomorSorter() throws IOException {
        super("Nomor Sorter — Sudah WA / Belum WA");
        Files.createDirectories(directory);
        if (!Files.exists(directory.resolve("shared-format.marker"))) migrateOldDates();
        migrateDeviceDates();
        migrateNumbersToEightFormat();
        loadDeviceCount();
        Files.writeString(directory.resolve("shared-format.marker"), "NomorSorter shared daily database v1\n", StandardCharsets.UTF_8);
        openDate(activeDate);
        // Basis data dari paket versi sebelumnya tetap dapat dipakai.
        Path legacyYes = directory.resolve("sudah-wa.db"), legacyNo = directory.resolve("belum-wa.db");
        boolean legacy = Files.exists(legacyYes) || Files.exists(legacyNo);
        read(legacyYes, yes, yesCom); read(legacyNo, no, noCom);
        no.removeAll(yes); // Nomor tidak boleh berada di kedua kelompok.
        noCom.keySet().retainAll(no);
        if (legacy) {
            save();
            if (Files.exists(legacyYes)) Files.move(legacyYes, directory.resolve("sudah-wa.db.migrated"), StandardCopyOption.REPLACE_EXISTING);
            if (Files.exists(legacyNo)) Files.move(legacyNo, directory.resolve("belum-wa.db.migrated"), StandardCopyOption.REPLACE_EXISTING);
        }
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(940, 640));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        root.setBackground(BACKGROUND);
        setContentPane(root);
        JLabel title = new JLabel("Nomor Sorter");
        title.setFont(new Font("SansSerif", Font.BOLD, 29));
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false); top.add(title, BorderLayout.NORTH);
        input.setLineWrap(true); input.setWrapStyleWord(true);
        input.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel entry = new RoundedPanel(new BorderLayout(0, 12));
        entry.add(new JLabel("Tempel baris data berisi nomor dan penanda WA (atau impor TXT/CSV):"), BorderLayout.NORTH);
        entry.add(new JScrollPane(input), BorderLayout.CENTER);
        JPanel entryButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        entryButtons.setOpaque(false);
        JButton addYes = button("Masukkan ke Sudah WA", new Color(27, 137, 93));
        JButton addNo = button("Masukkan ke Belum WA", new Color(56, 105, 185));
        JButton autoSort = button("Sortir otomatis dari tulisan WA", new Color(16, 159, 128));
        JButton importFile = button("Impor TXT/CSV", new Color(85, 93, 110));
        autoSort.addActionListener(e -> sortAutomatically());
        addYes.addActionListener(e -> add(input.getText(), true));
        addNo.addActionListener(e -> add(input.getText(), false));
        importFile.addActionListener(e -> importText());
        entryButtons.add(autoSort); entryButtons.add(addYes); entryButtons.add(addNo); entryButtons.add(importFile);
        entry.add(entryButtons, BorderLayout.SOUTH);
        top.add(entry, BorderLayout.CENTER);
        JPanel devicePage = new JPanel(new BorderLayout(14, 14));
        devicePage.setOpaque(false);
        devicePage.add(top, BorderLayout.NORTH);

        JPanel sidebar = new RoundedPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(178, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(23, 14, 20, 14));
        JLabel databaseTitle = new JLabel("Database bersama");
        databaseTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        databaseTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(databaseTitle);
        sidebar.add(Box.createVerticalStrut(16));
        JToggleButton showDevices = toggle("Device");
        JToggleButton showNo = toggle("Belum WA");
        JToggleButton showYes = toggle("Sudah WA");
        ButtonGroup menu = new ButtonGroup();
        showDevices.setSelected(true);
        showDevices.setForeground(BACKGROUND);
        for (JToggleButton nav : new JToggleButton[]{showDevices, showNo, showYes}) {
            menu.add(nav);
            nav.addItemListener(e -> nav.setForeground(nav.isSelected() ? BACKGROUND : TEXT));
            nav.setAlignmentX(Component.LEFT_ALIGNMENT);
            nav.setMaximumSize(new Dimension(149, 42));
            sidebar.add(nav);
            sidebar.add(Box.createVerticalStrut(8));
        }
        showNo.addActionListener(e -> showDatabase(false));
        showYes.addActionListener(e -> showDatabase(true));
        showDevices.addActionListener(e -> pageLayout.show(pages, "devices"));
        sidebar.add(Box.createVerticalStrut(12));
        JButton exportNumbers = button("Ekspor nomor", new Color(85, 93, 110));
        exportNumbers.setToolTipText("Ekspor nomor dari beberapa tanggal dan device ke Notepad (.txt) atau Excel (.xlsx)");
        exportNumbers.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportNumbers.setMaximumSize(new Dimension(149, 42));
        exportNumbers.addActionListener(e -> showExportDialog(Set.of(activeDate), null, null));
        sidebar.add(exportNumbers);
        sidebar.add(Box.createVerticalGlue());
        JButton checkUpdate = button("Periksa pembaruan", INPUT);
        checkUpdate.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkUpdate.setMaximumSize(new Dimension(149, 42));
        checkUpdate.addActionListener(e -> checkForUpdates(true));
        sidebar.add(checkUpdate);
        sidebar.add(Box.createVerticalStrut(8));
        JLabel versionLabel = new JLabel("Versi " + APP_VERSION);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(versionLabel);
        root.add(sidebar, BorderLayout.WEST);

        for (int i = 0; i < 2; i++) {
            models[i] = new DefaultTableModel(new Object[]{"No.", "COM", "Nomor telepon"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            tables[i] = new JTable(models[i]);
            tables[i].setRowHeight(30);
            tables[i].setFont(new Font("SansSerif", Font.PLAIN, 14));
            tables[i].getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
            tables[i].getColumnModel().getColumn(0).setMaxWidth(65);
            tables[i].getColumnModel().getColumn(1).setMaxWidth(105);
            tables[i].setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            sorters[i] = new TableRowSorter<>(models[i]);
            tables[i].setRowSorter(sorters[i]);
            styleTable(tables[i]);
            final int tableIndex = i;
            tables[i].addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    int row = tables[tableIndex].rowAtPoint(e.getPoint());
                    if (row >= 0) copyText((String) models[tableIndex].getValueAt(tables[tableIndex].convertRowIndexToModel(row), 2));
                }
            });
            tabs.addTab(i == 0 ? "Sudah WA" : "Belum WA", new JScrollPane(tables[i]));
        }
        tabs.addChangeListener(e -> { filter(); updateCount(); });
        JPanel list = new RoundedPanel(new BorderLayout(0, 12));
        JPanel controls = new JPanel(new BorderLayout(0, 8));
        controls.setOpaque(false);
        devicesPanel.setOpaque(false);
        rebuildDeviceButtons();
        controls.add(devicesPanel, BorderLayout.NORTH);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("Tanggal:"));
        datePicker.setPreferredSize(new Dimension(130, 29));
        toolbar.add(datePicker);
        JButton manualDate = button("Pilih dari kalender", new Color(57, 113, 176));
        manualDate.addActionListener(e -> showCalendar(manualDate, activeDate, date -> {
            try { switchInputDate(date); }
            catch (IOException ex) { error("Gagal membuka tanggal input: " + ex.getMessage()); }
        }));
        toolbar.add(manualDate);
        refreshDates();
        datePicker.addActionListener(e -> {
            String selected = (String) datePicker.getSelectedItem();
            if (!updatingDates && selected != null && !selected.equals(activeDate)) {
                try { switchInputDate(selected); }
                catch (IOException ex) { error("Gagal membuka tanggal: " + ex.getMessage()); datePicker.setSelectedItem(activeDate); }
            }
        });
        toolbar.add(new JLabel("Cari:")); toolbar.add(search);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });
        controls.add(toolbar, BorderLayout.SOUTH);
        list.add(controls, BorderLayout.NORTH);
        list.add(tabs, BorderLayout.CENTER);
        devicePage.add(list, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton delete = button("Hapus nomor terpilih", new Color(173, 66, 69));
        delete.setToolTipText("Sembunyikan dari riwayat device; database utama tetap tersimpan");
        JButton clearHistory = button("Kosongkan riwayat tanggal ini", new Color(137, 68, 83));
        JButton restoreHistory = button("Pulihkan riwayat tanggal ini", new Color(67, 101, 133));
        JButton export = button("Ekspor TXT/Excel", new Color(85, 93, 110));
        JButton copyAll = button("Salin semua nomor tanggal ini", new Color(32, 124, 139));
        JButton copySelected = button("Salin nomor terpilih", new Color(57, 113, 176));
        delete.addActionListener(e -> hideSelectedFromHistory());
        clearHistory.addActionListener(e -> clearHistory());
        restoreHistory.addActionListener(e -> restoreHistory());
        export.addActionListener(e -> showExportDialog(Set.of(activeDate), Set.of(activeDevice), Set.of(tabs.getSelectedIndex() == 0)));
        copyAll.addActionListener(e -> copyAll());
        copySelected.addActionListener(e -> copySelectedFromDevice());
        actions.add(copyAll); actions.add(copySelected); actions.add(delete); actions.add(export);
        bottom.add(actions, BorderLayout.NORTH);
        JPanel historyActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        historyActions.setOpaque(false);
        historyActions.add(clearHistory);
        historyActions.add(restoreHistory);
        bottom.add(historyActions, BorderLayout.CENTER);
        bottom.add(count, BorderLayout.SOUTH);
        devicePage.add(bottom, BorderLayout.SOUTH);
        pages.setOpaque(false);
        pages.add(devicePage, "devices");
        pages.add(createDatabasePage(false), "belum");
        pages.add(createDatabasePage(true), "sudah");
        root.add(pages, BorderLayout.CENTER);
        pageLayout.show(pages, "devices");
        styleTree(root);
        refresh();
        javax.swing.Timer dayChangeTimer = new javax.swing.Timer(15_000, e -> updateDateAtMidnight());
        dayChangeTimer.start();
        javax.swing.Timer updateTimer = new javax.swing.Timer(2_500, e -> { reportFailedUpdate(); checkForUpdates(false); });
        updateTimer.setRepeats(false);
        updateTimer.start();
    }

    private JButton button(String label, Color color) {
        JButton b = new JButton(label);
        b.setBackground(color); b.setForeground(TEXT);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setUI(new RoundedButtonUI());
        b.setOpaque(false); b.setContentAreaFilled(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(11, 15, 11, 15));
        return b;
    }

    private JToggleButton toggle(String label) {
        JToggleButton b = new JToggleButton(label);
        b.setBackground(INPUT);
        b.setForeground(TEXT);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setUI(new RoundedButtonUI());
        b.setOpaque(false); b.setContentAreaFilled(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        return b;
    }

    private static class RoundedPanel extends JPanel {
        RoundedPanel() { super(); setOpaque(false); setBackground(PANEL); }
        RoundedPanel(LayoutManager layout) {
            super(layout); setOpaque(false); setBackground(PANEL);
            setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(getBackground());
            graphics.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            graphics.setColor(BORDER);
            graphics.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            graphics.dispose();
            super.paintComponent(g);
        }
    }

    // FlowLayout yang memindahkan tombol ke baris baru bila device terlalu banyak untuk satu baris.
    private static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
        @Override public Dimension minimumLayoutSize(Container target) {
            Dimension size = layoutSize(target, false);
            size.width -= getHgap() + 1;
            return size;
        }
        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                Container container = target;
                while (container.getSize().width == 0 && container.getParent() != null) container = container.getParent();
                int targetWidth = container.getSize().width == 0 ? Integer.MAX_VALUE : container.getSize().width;
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + getHgap() * 2);
                int width = 0, height = 0, rowWidth = 0, rowHeight = 0;
                for (Component component : target.getComponents()) {
                    if (!component.isVisible()) continue;
                    Dimension size = preferred ? component.getPreferredSize() : component.getMinimumSize();
                    if (rowWidth > 0 && rowWidth + getHgap() + size.width > maxWidth) {
                        width = Math.max(width, rowWidth);
                        height += rowHeight + getVgap();
                        rowWidth = 0; rowHeight = 0;
                    }
                    if (rowWidth > 0) rowWidth += getHgap();
                    rowWidth += size.width;
                    rowHeight = Math.max(rowHeight, size.height);
                }
                width = Math.max(width, rowWidth);
                height += rowHeight;
                return new Dimension(width + insets.left + insets.right + getHgap() * 2,
                    height + insets.top + insets.bottom + getVgap() * 2);
            }
        }
    }

    private static class RoundedButtonUI extends BasicButtonUI {
        @Override public void paint(Graphics g, JComponent component) {
            AbstractButton button = (AbstractButton) component;
            Color fill = button.isSelected() ? ACCENT : button.getBackground();
            if (button.getModel().isPressed()) fill = fill.darker();
            else if (button.getModel().isRollover()) fill = new Color(
                Math.min(255, fill.getRed() + 13), Math.min(255, fill.getGreen() + 13), Math.min(255, fill.getBlue() + 13));
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(fill);
            graphics.fillRoundRect(0, 0, component.getWidth() - 1, component.getHeight() - 1, 15, 15);
            graphics.dispose();
            super.paint(g, component);
        }
    }

    private void styleTable(JTable table) {
        table.setBackground(INPUT);
        table.setForeground(TEXT);
        table.setSelectionBackground(new Color(35, 85, 102));
        table.setSelectionForeground(TEXT);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.getTableHeader().setPreferredSize(new Dimension(10, 38));
        table.getTableHeader().setDefaultRenderer((t, value, selected, focus, row, column) -> {
            JLabel label = new JLabel("  " + value);
            label.setOpaque(true);
            label.setBackground(new Color(28, 47, 72));
            label.setForeground(MUTED);
            label.setFont(new Font("SansSerif", Font.BOLD, 13));
            return label;
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                       boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                setBackground(selected ? t.getSelectionBackground() : (row % 2 == 0 ? INPUT : PANEL));
                setForeground(TEXT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                return this;
            }
        });
    }

    private void styleTree(Component component) {
        if (component instanceof JLabel label) {
            label.setForeground(TEXT);
            if (label.getFont().getSize() < 14) label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        }
        if (component instanceof JTextArea area) {
            area.setBackground(INPUT); area.setForeground(TEXT); area.setCaretColor(ACCENT);
            area.setSelectionColor(new Color(40, 91, 112));
            area.setFont(new Font("Monospaced", Font.PLAIN, 13));
            area.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        }
        if (component instanceof JTextField field) {
            field.setBackground(INPUT); field.setForeground(TEXT); field.setCaretColor(ACCENT);
            field.setSelectionColor(new Color(40, 91, 112));
            field.setFont(new Font("SansSerif", Font.PLAIN, 14));
            field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(6, 9, 6, 9)));
        }
        if (component instanceof JComboBox<?> combo) {
            combo.setBackground(INPUT); combo.setForeground(TEXT);
            combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                          boolean selected, boolean focused) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                    label.setBackground(selected ? new Color(40, 91, 112) : INPUT);
                    label.setForeground(TEXT);
                    label.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 7));
                    return label;
                }
            });
        }
        if (component instanceof JScrollPane scroll) {
            scroll.setBorder(BorderFactory.createLineBorder(BORDER));
            scroll.getViewport().setBackground(INPUT);
        }
        if (component instanceof JTabbedPane pane) {
            pane.setBackground(PANEL); pane.setForeground(TEXT);
            pane.setFont(new Font("SansSerif", Font.BOLD, 13));
            pane.setUI(new BasicTabbedPaneUI() {
                @Override protected void installDefaults() {
                    super.installDefaults();
                    tabInsets = new Insets(10, 16, 10, 16);
                }
                @Override protected void paintTabBackground(Graphics g, int place, int index, int x, int y,
                                                                 int width, int height, boolean selected) {
                    g.setColor(selected ? new Color(33, 78, 79) : PANEL);
                    g.fillRoundRect(x, y, width, height, 12, 12);
                }
                @Override protected void paintTabBorder(Graphics g, int place, int index, int x, int y,
                                                             int width, int height, boolean selected) { }
                @Override protected void paintContentBorder(Graphics g, int place, int selected) { }
            });
        }
        if (component instanceof Container container)
            for (Component child : container.getComponents()) styleTree(child);
    }

    private static void read(Path file, Set<String> output, Map<String, String> coms) throws IOException {
        if (!Files.exists(file)) return;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String[] columns = line.split("\\t", 2);
            String normalized = normalize(columns[0]);
            if (normalized != null) {
                output.add(normalized);
                if (columns.length > 1 && columns[1].matches("COM[0-9]+")) coms.put(normalized, columns[1]);
            }
        }
    }

    private static String comIn(String line) {
        Matcher matcher = COM_TAG.matcher(line);
        return matcher.find() ? "COM" + matcher.group(1) : null;
    }

    private static String today() { return LocalDate.now().toString(); }

    private Path deviceDirectory() { return directory.resolve("device-" + activeDevice); }

    private Path hiddenFolder(String date) {
        return directory.resolve("riwayat-tersembunyi").resolve("device-" + activeDevice).resolve(date);
    }

    private void saveHidden() throws IOException {
        Path folder = hiddenFolder(activeDate);
        Files.createDirectories(folder);
        write(folder.resolve("sudah-wa.db"), hiddenYes, Collections.emptyMap());
        write(folder.resolve("belum-wa.db"), hiddenNo, Collections.emptyMap());
    }

    private void migrateOldDates() throws IOException {
        Path destination = directory.resolve("device-1");
        Files.createDirectories(destination);
        try (DirectoryStream<Path> folders = Files.newDirectoryStream(directory)) {
            for (Path folder : folders) {
                if (!Files.isDirectory(folder)) continue;
                String name = folder.getFileName().toString();
                try { LocalDate.parse(name); }
                catch (java.time.format.DateTimeParseException ignored) { continue; }
                Path target = destination.resolve(name);
                if (!Files.exists(target)) Files.move(folder, target);
                else {
                    for (String status : new String[]{"sudah-wa.db", "belum-wa.db"}) {
                        Set<String> numbers = new LinkedHashSet<>(), existing = new LinkedHashSet<>();
                        Map<String, String> coms = new LinkedHashMap<>(), existingCom = new LinkedHashMap<>();
                        read(folder.resolve(status), numbers, coms);
                        read(target.resolve(status), existing, existingCom);
                        existing.addAll(numbers);
                        for (Map.Entry<String, String> entry : coms.entrySet()) existingCom.putIfAbsent(entry.getKey(), entry.getValue());
                        write(target.resolve(status), existing, existingCom);
                    }
                    Files.move(folder, directory.resolve(name + ".migrated"));
                }
            }
        }
    }

    private void migrateDeviceDates() throws IOException {
        for (int device = 1; device <= 5; device++) {
            Path oldDevice = directory.resolve("device-" + device);
            if (!Files.isDirectory(oldDevice)) continue;
            try (DirectoryStream<Path> days = Files.newDirectoryStream(oldDevice)) {
                for (Path oldDay : days) {
                    if (!Files.isDirectory(oldDay)) continue;
                    String date = oldDay.getFileName().toString();
                    try { LocalDate.parse(date); }
                    catch (java.time.format.DateTimeParseException ignored) { continue; }
                    Path sharedDay = directory.resolve(date);
                    Files.createDirectories(sharedDay);
                    for (String status : new String[]{"sudah-wa.db", "belum-wa.db"}) {
                        Set<String> oldNumbers = new LinkedHashSet<>();
                        Map<String, String> oldCom = new LinkedHashMap<>();
                        read(oldDay.resolve(status), oldNumbers, oldCom);
                        mergeStatus(sharedDay.resolve(status), device, oldNumbers, oldCom, false);
                    }
                    Files.move(oldDay, oldDevice.resolve(date + ".migrated"));
                }
            }
        }
    }

    private static void readShared(Path file, Map<Integer, LinkedHashSet<String>> numbers,
                                   Map<Integer, Map<String, String>> coms) throws IOException {
        if (!Files.exists(file)) return;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String[] columns = line.split("\\t", -1);
            String number = normalize(columns[0]);
            if (number == null) continue;
            int device = 1;
            if (columns.length > 2) {
                try { device = Integer.parseInt(columns[2]); } catch (NumberFormatException ignored) { continue; }
            }
            if (device < 1) continue;
            numbers.computeIfAbsent(device, ignored -> new LinkedHashSet<>()).add(number);
            if (columns.length > 1 && columns[1].matches("COM[0-9]+"))
                coms.computeIfAbsent(device, ignored -> new LinkedHashMap<>()).put(number, columns[1]);
        }
    }

    private static void mergeStatus(Path file, int device, Set<String> entries,
                                    Map<String, String> entryComs, boolean replaceDevice) throws IOException {
        Map<Integer, LinkedHashSet<String>> all = new TreeMap<>();
        Map<Integer, Map<String, String>> allComs = new TreeMap<>();
        readShared(file, all, allComs);
        LinkedHashSet<String> current = all.computeIfAbsent(device, ignored -> new LinkedHashSet<>());
        Map<String, String> currentCom = allComs.computeIfAbsent(device, ignored -> new LinkedHashMap<>());
        if (replaceDevice) { current.clear(); currentCom.clear(); }
        current.addAll(entries);
        for (Map.Entry<String, String> pair : entryComs.entrySet()) currentCom.putIfAbsent(pair.getKey(), pair.getValue());
        writeShared(file, all, allComs);
    }

    private static void writeShared(Path file, Map<Integer, LinkedHashSet<String>> all,
                                    Map<Integer, Map<String, String>> allComs) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<Integer, LinkedHashSet<String>> group : all.entrySet())
            for (String number : group.getValue())
                lines.add(number + "\t" + allComs.getOrDefault(group.getKey(), Collections.emptyMap()).getOrDefault(number, "") + "\t" + group.getKey());
        Path temp = Files.createTempFile(file.getParent(), "nomor-gabungan-", ".tmp");
        try {
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    private static int deleteShared(Path file, Map<Integer, Set<String>> selected, boolean entireDate) throws IOException {
        Map<Integer, LinkedHashSet<String>> all = new TreeMap<>();
        Map<Integer, Map<String, String>> coms = new TreeMap<>();
        readShared(file, all, coms);
        int removed = 0;
        for (Map.Entry<Integer, LinkedHashSet<String>> group : all.entrySet()) {
            Set<String> targets = selected.getOrDefault(group.getKey(), Collections.emptySet());
            Iterator<String> items = group.getValue().iterator();
            while (items.hasNext()) {
                String number = items.next();
                if (entireDate || targets.contains(number)) {
                    items.remove();
                    Map<String, String> deviceComs = coms.get(group.getKey());
                    if (deviceComs != null) deviceComs.remove(number);
                    removed++;
                }
            }
        }
        if (removed > 0) writeShared(file, all, coms);
        return removed;
    }

    private void openDate(String date) throws IOException {
        LocalDate.parse(date); // Pastikan nama folder selalu tanggal yang valid.
        Path folder = directory.resolve(date);
        Files.createDirectories(folder);
        Map<Integer, LinkedHashSet<String>> allYes = new TreeMap<>(), allNo = new TreeMap<>();
        Map<Integer, Map<String, String>> allYesCom = new TreeMap<>(), allNoCom = new TreeMap<>();
        readShared(folder.resolve("sudah-wa.db"), allYes, allYesCom);
        readShared(folder.resolve("belum-wa.db"), allNo, allNoCom);
        LinkedHashSet<String> newYes = allYes.getOrDefault(activeDevice, new LinkedHashSet<>());
        LinkedHashSet<String> newNo = allNo.getOrDefault(activeDevice, new LinkedHashSet<>());
        Map<String, String> newYesCom = allYesCom.getOrDefault(activeDevice, new LinkedHashMap<>());
        Map<String, String> newNoCom = allNoCom.getOrDefault(activeDevice, new LinkedHashMap<>());
        newNo.removeAll(newYes);
        newNoCom.keySet().retainAll(newNo);
        yes.clear(); yes.addAll(newYes);
        no.clear(); no.addAll(newNo);
        yesCom.clear(); yesCom.putAll(newYesCom);
        noCom.clear(); noCom.putAll(newNoCom);
        Set<String> newHiddenYes = new LinkedHashSet<>(), newHiddenNo = new LinkedHashSet<>();
        read(hiddenFolder(date).resolve("sudah-wa.db"), newHiddenYes, new LinkedHashMap<>());
        read(hiddenFolder(date).resolve("belum-wa.db"), newHiddenNo, new LinkedHashMap<>());
        hiddenYes.clear(); hiddenYes.addAll(newHiddenYes);
        hiddenNo.clear(); hiddenNo.addAll(newHiddenNo);
        activeDate = date;
        yesFile = folder.resolve("sudah-wa.db");
        noFile = folder.resolve("belum-wa.db");
    }

    private void refreshDates() {
        TreeSet<String> dates = new TreeSet<>(Comparator.reverseOrder());
        dates.add(today());
        try (DirectoryStream<Path> folders = Files.newDirectoryStream(directory)) {
            for (Path folder : folders) {
                if (!Files.isDirectory(folder)) continue;
                try { LocalDate.parse(folder.getFileName().toString()); dates.add(folder.getFileName().toString()); }
                catch (java.time.format.DateTimeParseException ignored) { }
            }
        } catch (IOException ex) { error("Gagal membaca daftar tanggal: " + ex.getMessage()); }
        updatingDates = true;
        try {
            datePicker.removeAllItems();
            for (String date : dates) datePicker.addItem(date);
            datePicker.setSelectedItem(activeDate);
        } finally { updatingDates = false; }
    }

    private void switchDevice(int number) {
        if (!updateDateAtMidnight()) return;
        if (number == activeDevice) return;
        String previousDraft = input.getText();
        int previousDevice = activeDevice;
        try {
            activeDevice = number;
            openDate(deviceDates.get(number - 1));
            drafts.set(previousDevice - 1, previousDraft);
            input.setText(drafts.get(number - 1) == null ? "" : drafts.get(number - 1));
            refreshDates();
            refresh();
        } catch (IOException ex) {
            activeDevice = previousDevice;
            error("Gagal membuka Device " + number + ": " + ex.getMessage());
        }
    }

    private void loadDeviceCount() {
        Path file = directory.resolve("jumlah-device.txt");
        if (Files.exists(file)) {
            try { deviceCount = Math.max(1, Integer.parseInt(Files.readString(file, StandardCharsets.UTF_8).trim())); }
            catch (IOException | NumberFormatException ignored) { deviceCount = 5; }
        }
        drafts.clear(); deviceDates.clear();
        for (int i = 0; i < deviceCount; i++) { drafts.add(null); deviceDates.add(today()); }
    }

    private void saveDeviceCount() throws IOException {
        Files.writeString(directory.resolve("jumlah-device.txt"), deviceCount + "\n", StandardCharsets.UTF_8);
    }

    private void rebuildDeviceButtons() {
        devicesPanel.removeAll();
        deviceGroup = new ButtonGroup();
        for (int number = 1; number <= deviceCount; number++) {
            final int deviceNumber = number;
            JToggleButton device = new JToggleButton("Device " + number);
            device.setFont(new Font("SansSerif", Font.BOLD, 13));
            device.setPreferredSize(new Dimension(112, 34));
            device.setFocusPainted(false);
            device.setSelected(number == activeDevice);
            device.setBackground(INPUT);
            device.setForeground(number == activeDevice ? BACKGROUND : TEXT);
            device.setUI(new RoundedButtonUI());
            device.setOpaque(false);
            device.setContentAreaFilled(false);
            device.setBorder(BorderFactory.createEmptyBorder(8, 13, 8, 13));
            device.addItemListener(e -> device.setForeground(device.isSelected() ? BACKGROUND : TEXT));
            device.addActionListener(e -> switchDevice(deviceNumber));
            deviceGroup.add(device);
            devicesPanel.add(device);
        }
        JButton addDevice = button("+ Tambah device", new Color(27, 137, 93));
        addDevice.setBorder(BorderFactory.createEmptyBorder(8, 13, 8, 13));
        addDevice.addActionListener(e -> addDevice());
        devicesPanel.add(addDevice);
        if (deviceCount > 1) {
            JButton removeDevice = button("− Hapus Device " + deviceCount, new Color(137, 68, 83));
            removeDevice.setToolTipText("Hanya bisa jika device terakhir tidak memiliki catatan di database");
            removeDevice.setBorder(BorderFactory.createEmptyBorder(8, 13, 8, 13));
            removeDevice.addActionListener(e -> removeLastDevice());
            devicesPanel.add(removeDevice);
        }
        devicesPanel.revalidate();
        devicesPanel.repaint();
    }

    private void addDevice() {
        deviceCount++;
        drafts.add(null);
        deviceDates.add(today());
        try { saveDeviceCount(); }
        catch (IOException ex) {
            deviceCount--; drafts.remove(deviceCount); deviceDates.remove(deviceCount);
            error("Gagal menyimpan jumlah device: " + ex.getMessage());
            return;
        }
        switchDevice(deviceCount);
        rebuildDeviceButtons();
    }

    private void removeLastDevice() {
        int last = deviceCount;
        int records;
        try { records = countDeviceRecords(last); }
        catch (IOException ex) { error("Gagal memeriksa data Device " + last + ": " + ex.getMessage()); return; }
        if (records > 0) {
            error("Device " + last + " masih memiliki " + records + " catatan di database.\n"
                + "Device yang masih berisi data tidak dapat dihapus agar nomornya tidak hilang dari tampilan.");
            return;
        }
        String draft = last == activeDevice ? input.getText() : drafts.get(last - 1);
        String message = "Hapus Device " + last + " dari daftar?"
            + (draft != null && !draft.isBlank() ? "\nTeks input yang belum diproses pada device ini akan hilang." : "");
        if (JOptionPane.showConfirmDialog(this, message, "Hapus device", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        if (activeDevice == last) {
            switchDevice(last - 1);
            if (activeDevice == last) return;
        }
        deviceCount--;
        drafts.remove(last - 1);
        deviceDates.remove(last - 1);
        try { saveDeviceCount(); }
        catch (IOException ex) { error("Gagal menyimpan jumlah device: " + ex.getMessage()); }
        rebuildDeviceButtons();
    }

    private int countDeviceRecords(int device) throws IOException {
        int total = 0;
        try (DirectoryStream<Path> folders = Files.newDirectoryStream(directory)) {
            for (Path folder : folders) {
                if (!Files.isDirectory(folder)) continue;
                try { LocalDate.parse(folder.getFileName().toString()); }
                catch (java.time.format.DateTimeParseException ignored) { continue; }
                for (String status : new String[]{"sudah-wa.db", "belum-wa.db"}) {
                    Map<Integer, LinkedHashSet<String>> all = new TreeMap<>();
                    readShared(folder.resolve(status), all, new TreeMap<>());
                    total += all.getOrDefault(device, new LinkedHashSet<>()).size();
                }
            }
        }
        return total;
    }

    private void switchInputDate(String date) throws IOException {
        LocalDate.parse(date);
        openDate(date);
        deviceDates.set(activeDevice - 1, date);
        refreshDates();
        refresh();
    }

    private void showCalendar(JButton anchor, String selectedDate, java.util.function.Consumer<String> onSelect) {
        LocalDate selected = LocalDate.parse(selectedDate);
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(BORDER));
        JPanel calendar = new JPanel(new BorderLayout(5, 8));
        calendar.setBackground(PANEL);
        calendar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        popup.add(calendar);
        YearMonth[] visibleMonth = {YearMonth.from(selected)};
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            calendar.removeAll();
            JPanel heading = new JPanel(new BorderLayout());
            heading.setOpaque(false);
            JLabel month = new JLabel(visibleMonth[0].atDay(1).format(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"))), SwingConstants.CENTER);
            month.setForeground(TEXT);
            month.setFont(new Font("SansSerif", Font.BOLD, 15));
            JButton previous = button("‹", INPUT), next = button("›", INPUT);
            previous.addActionListener(e -> { visibleMonth[0] = visibleMonth[0].minusMonths(1); render[0].run(); });
            next.addActionListener(e -> { visibleMonth[0] = visibleMonth[0].plusMonths(1); render[0].run(); });
            heading.add(previous, BorderLayout.WEST);
            heading.add(month, BorderLayout.CENTER);
            heading.add(next, BorderLayout.EAST);
            calendar.add(heading, BorderLayout.NORTH);

            JPanel days = new JPanel(new GridLayout(0, 7, 2, 2));
            days.setOpaque(false);
            for (String dayName : new String[]{"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"}) {
                JLabel name = new JLabel(dayName, SwingConstants.CENTER);
                name.setForeground(MUTED);
                days.add(name);
            }
            int offset = visibleMonth[0].atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < offset; i++) days.add(new JLabel());
            for (int day = 1; day <= visibleMonth[0].lengthOfMonth(); day++) {
                LocalDate choice = visibleMonth[0].atDay(day);
                JButton date = button(Integer.toString(day), choice.equals(selected) ? ACCENT : INPUT);
                date.setForeground(choice.equals(selected) ? BACKGROUND : TEXT);
                date.setFont(new Font("SansSerif", Font.BOLD, 13));
                date.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                date.setMargin(new Insets(0, 0, 0, 0));
                date.setPreferredSize(new Dimension(40, 34));
                date.addActionListener(e -> { popup.setVisible(false); onSelect.accept(choice.toString()); });
                days.add(date);
            }
            calendar.add(days, BorderLayout.CENTER);
            JButton todayButton = button("Hari ini", INPUT);
            todayButton.addActionListener(e -> { popup.setVisible(false); onSelect.accept(today()); });
            calendar.add(todayButton, BorderLayout.SOUTH);
            popup.pack();
            calendar.revalidate();
            calendar.repaint();
        };
        render[0].run();
        popup.show(anchor, 0, anchor.getHeight());
    }

    private boolean updateDateAtMidnight() {
        String current = today();
        if (current.equals(lastKnownDay)) return true;
        try {
            openDate(current);
            Collections.fill(deviceDates, current);
            lastKnownDay = current;
            refreshDates();
            refresh();
            return true;
        } catch (IOException ex) {
            error("Gagal mengganti tanggal otomatis: " + ex.getMessage());
            return false;
        }
    }

    private static String normalize(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("62")) digits = digits.substring(2);
        else if (digits.startsWith("0")) digits = digits.substring(1);
        if (!digits.startsWith("8") || digits.length() < 9 || digits.length() > 13) return null;
        return digits;
    }

    private void migrateNumbersToEightFormat() throws IOException {
        migrateDateFolders(directory);
        Path hiddenRoot = directory.resolve("riwayat-tersembunyi");
        if (Files.isDirectory(hiddenRoot)) {
            try (DirectoryStream<Path> devices = Files.newDirectoryStream(hiddenRoot, "device-*")) {
                for (Path device : devices) if (Files.isDirectory(device)) migrateDateFolders(device);
            }
        }
    }

    private void migrateDateFolders(Path parent) throws IOException {
        try (DirectoryStream<Path> dates = Files.newDirectoryStream(parent)) {
            for (Path day : dates) {
                if (!Files.isDirectory(day)) continue;
                try { LocalDate.parse(day.getFileName().toString()); }
                catch (java.time.format.DateTimeParseException ignored) { continue; }
                for (String status : new String[]{"sudah-wa.db", "belum-wa.db"}) {
                    Path file = day.resolve(status);
                    if (!Files.isRegularFile(file)) continue;
                    convertStoredFile(file);
                }
            }
        }
    }

    private static void convertStoredFile(Path file) throws IOException {
        List<String> original = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> converted = new ArrayList<>();
        boolean changed = false;
        for (String line : original) {
            String[] columns = line.split("\\t", 2);
            String normalized = normalize(columns[0]);
            if (normalized != null && !normalized.equals(columns[0])) {
                converted.add(normalized + (columns.length > 1 ? "\t" + columns[1] : ""));
                changed = true;
            } else converted.add(line);
        }
        if (!changed) return;
        Path parent = file.getParent();
        Path backup = parent.resolve(file.getFileName() + ".sebelum-format-8.bak");
        if (!Files.exists(backup)) Files.copy(file, backup);
        Path temporary = Files.createTempFile(parent, "format-8-", ".tmp");
        try {
            Files.write(temporary, converted, StandardCharsets.UTF_8);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static LinkedHashSet<String> extract(String text) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        Matcher matcher = PHONE.matcher(text);
        while (matcher.find()) {
            String number = normalize(matcher.group());
            if (number != null) found.add(number);
        }
        return found;
    }

    private static void write(Path file, Set<String> items, Map<String, String> coms) throws IOException {
        Path temp = Files.createTempFile(file.getParent(), "nomor-", ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            for (String number : items) lines.add(number + (coms.containsKey(number) ? "\t" + coms.get(number) : ""));
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    private void save() throws IOException {
        mergeStatus(yesFile, activeDevice, yes, yesCom, true);
        mergeStatus(noFile, activeDevice, no, noCom, true);
    }
    private void persist() {
        try { save(); saveHidden(); refresh(); }
        catch (IOException ex) { error("Gagal menyimpan data: " + ex.getMessage()); }
    }

    private void add(String text, boolean toYes) {
        LinkedHashSet<String> numbers = extract(text);
        if (numbers.isEmpty()) { error("Tidak ditemukan nomor Indonesia yang valid (contoh: 081234567890)."); return; }
        if (!updateDateAtMidnight()) return;
        Set<String> target = toYes ? yes : no;
        Set<String> other = toYes ? no : yes;
        Map<String, String> targetCom = toYes ? yesCom : noCom, otherCom = toYes ? noCom : yesCom;
        for (String line : text.split("\\R")) {
            String com = comIn(line);
            for (String number : extract(line)) {
                other.remove(number);
                String prior = otherCom.remove(number);
                target.add(number);
                hiddenYes.remove(number);
                hiddenNo.remove(number);
                if (com != null) targetCom.put(number, com);
                else if (prior != null) targetCom.putIfAbsent(number, prior);
            }
        }
        persist(); input.setText("");
        tabs.setSelectedIndex(toYes ? 0 : 1);
        JOptionPane.showMessageDialog(this, numbers.size() + " nomor diproses.");
    }

    private void sortAutomatically() {
        if (extract(input.getText()).isEmpty()) { error("Tidak ditemukan nomor. Format yang diterima: 831234567890, 0831234567890, atau 62831234567890."); return; }
        if (!updateDateAtMidnight()) return;
        int found = 0, marked = 0, unmarked = 0;
        for (String line : input.getText().split("\\R")) {
            LinkedHashSet<String> numbers = extract(line);
            String com = comIn(line);
            boolean hasWa = WA_TAG.matcher(line).find() && !NO_WA_TAG.matcher(line).find();
            for (String number : numbers) {
                found++;
                hiddenYes.remove(number);
                hiddenNo.remove(number);
                if (hasWa) {
                    no.remove(number);
                    String prior = noCom.remove(number);
                    yes.add(number);
                    if (com != null) yesCom.put(number, com);
                    else if (prior != null) yesCom.putIfAbsent(number, prior);
                    marked++;
                } else {
                    yes.remove(number);
                    String prior = yesCom.remove(number);
                    no.add(number);
                    if (com != null) noCom.put(number, com);
                    else if (prior != null) noCom.putIfAbsent(number, prior);
                    unmarked++;
                }
            }
        }
        persist(); input.setText("");
        JOptionPane.showMessageDialog(this, "Diproses " + found + " baris nomor: " + marked + " bertanda WA, " + unmarked + " tanpa WA.\nNomor tanpa tulisan WA dimasukkan ke Belum WA.");
    }

    private void importText() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Teks / CSV", "txt", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try { input.setText(Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8)); }
        catch (IOException ex) { error("Gagal membaca berkas: " + ex.getMessage()); }
    }

    private void hideSelectedFromHistory() {
        int index = tabs.getSelectedIndex();
        JTable table = tables[index];
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) { error("Pilih nomor pada tabel terlebih dahulu."); return; }
        Set<String> hidden = index == 0 ? hiddenYes : hiddenNo;
        for (int row : rows) hidden.add(normalize((String) models[index].getValueAt(table.convertRowIndexToModel(row), 2)));
        try { saveHidden(); refresh(); }
        catch (IOException ex) { error("Gagal menyimpan riwayat: " + ex.getMessage()); }
    }

    private void clearHistory() {
        hiddenYes.addAll(yes);
        hiddenNo.addAll(no);
        try { saveHidden(); refresh(); }
        catch (IOException ex) { error("Gagal mengosongkan riwayat: " + ex.getMessage()); }
    }

    private void restoreHistory() {
        hiddenYes.clear();
        hiddenNo.clear();
        try { saveHidden(); refresh(); }
        catch (IOException ex) { error("Gagal memulihkan riwayat: " + ex.getMessage()); }
    }

    private record NumberRecord(String date, int device, String com, String number, boolean sudah) { }

    private static final String[] NUMBER_FORMATS = {"8xxxxxxxxx", "08xxxxxxxxx", "62xxxxxxxxx", "+62xxxxxxxxx"};

    private static String formatNumber(String number, int style) {
        return switch (style) {
            case 1 -> "0" + number;
            case 2 -> "62" + number;
            case 3 -> "+62" + number;
            default -> number;
        };
    }

    // Membaca seluruh database utama (semua tanggal, device, dan status), urut dari tanggal terlama.
    private List<NumberRecord> loadAllRecords() throws IOException {
        TreeSet<String> dates = new TreeSet<>();
        try (DirectoryStream<Path> folders = Files.newDirectoryStream(directory)) {
            for (Path folder : folders) {
                if (!Files.isDirectory(folder)) continue;
                try { LocalDate.parse(folder.getFileName().toString()); dates.add(folder.getFileName().toString()); }
                catch (java.time.format.DateTimeParseException ignored) { }
            }
        }
        List<NumberRecord> records = new ArrayList<>();
        for (String date : dates) {
            for (boolean sudah : new boolean[]{true, false}) {
                Map<Integer, LinkedHashSet<String>> all = new TreeMap<>();
                Map<Integer, Map<String, String>> allCom = new TreeMap<>();
                readShared(directory.resolve(date).resolve(sudah ? "sudah-wa.db" : "belum-wa.db"), all, allCom);
                for (Map.Entry<Integer, LinkedHashSet<String>> group : all.entrySet())
                    for (String number : group.getValue())
                        records.add(new NumberRecord(date, group.getKey(),
                            allCom.getOrDefault(group.getKey(), Collections.emptyMap()).getOrDefault(number, ""), number, sudah));
            }
        }
        records.sort(Comparator.comparing(NumberRecord::date).thenComparingInt(NumberRecord::device));
        return records;
    }

    private JCheckBox checkBox(String label, boolean selected) {
        JCheckBox box = new JCheckBox(label, selected);
        box.setOpaque(false); box.setForeground(TEXT); box.setFocusPainted(false);
        box.setFont(new Font("SansSerif", Font.PLAIN, 13));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        return box;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** presetDevices / presetStatus bernilai null berarti semua dicentang. */
    private void showExportDialog(Set<String> presetDates, Set<Integer> presetDevices, Set<Boolean> presetStatus) {
        List<NumberRecord> records;
        try { records = loadAllRecords(); }
        catch (IOException ex) { error("Gagal membaca database: " + ex.getMessage()); return; }
        TreeSet<String> dates = new TreeSet<>(Comparator.reverseOrder());
        TreeSet<Integer> deviceIds = new TreeSet<>();
        Map<String, Integer> perDate = new HashMap<>();
        for (int device = 1; device <= deviceCount; device++) deviceIds.add(device);
        for (NumberRecord record : records) {
            dates.add(record.date()); deviceIds.add(record.device());
            perDate.merge(record.date(), 1, Integer::sum);
        }
        dates.addAll(presetDates);
        Locale indonesia = Locale.forLanguageTag("id-ID");

        JDialog dialog = new JDialog(this, "Ekspor nomor", true);
        JPanel content = new JPanel(new BorderLayout(16, 14));
        content.setBackground(PANEL);
        content.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        dialog.setContentPane(content);

        // Kolom kiri: daftar tanggal yang bisa dicentang beberapa sekaligus.
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        left.add(sectionLabel("1. Pilih tanggal (boleh lebih dari satu)"), BorderLayout.NORTH);
        JPanel dateList = new JPanel();
        dateList.setLayout(new BoxLayout(dateList, BoxLayout.Y_AXIS));
        dateList.setBackground(INPUT);
        dateList.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        Map<String, JCheckBox> dateBoxes = new LinkedHashMap<>();
        for (String date : dates) {
            String day = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE", indonesia));
            JCheckBox box = checkBox(date + "   ·   " + day + "   ·   " + perDate.getOrDefault(date, 0) + " catatan", presetDates.contains(date));
            dateBoxes.put(date, box);
            dateList.add(box);
        }
        JScrollPane dateScroll = new JScrollPane(dateList);
        dateScroll.setPreferredSize(new Dimension(360, 330));
        dateScroll.getVerticalScrollBar().setUnitIncrement(16);
        left.add(dateScroll, BorderLayout.CENTER);
        JPanel quick = new JPanel(new GridLayout(2, 1, 0, 6));
        quick.setOpaque(false);
        JPanel monthRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        monthRow.setOpaque(false);
        Map<String, String> months = new LinkedHashMap<>();
        for (String date : dates) {
            String key = date.substring(0, 7);
            months.putIfAbsent(YearMonth.parse(key).atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", indonesia)), key);
        }
        JComboBox<String> monthPicker = new JComboBox<>(months.keySet().toArray(new String[0]));
        monthPicker.setPreferredSize(new Dimension(150, 30));
        JButton pickMonth = button("Centang bulan ini", new Color(57, 113, 176));
        pickMonth.setBorder(BorderFactory.createEmptyBorder(7, 11, 7, 11));
        monthRow.add(new JLabel("Bulan:")); monthRow.add(monthPicker); monthRow.add(pickMonth);
        JPanel allRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        allRow.setOpaque(false);
        JButton pickAll = button("Centang semua tanggal", INPUT), pickNone = button("Kosongkan", INPUT);
        pickAll.setBorder(BorderFactory.createEmptyBorder(7, 11, 7, 11));
        pickNone.setBorder(BorderFactory.createEmptyBorder(7, 11, 7, 11));
        allRow.add(pickAll); allRow.add(pickNone);
        quick.add(monthRow); quick.add(allRow);
        left.add(quick, BorderLayout.SOUTH);
        content.add(left, BorderLayout.WEST);

        // Kolom kanan: device, status, dan format berkas.
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.add(sectionLabel("2. Pilih device"));
        JPanel devicesRow = new JPanel(new GridLayout(0, 3, 6, 2));
        devicesRow.setOpaque(false);
        devicesRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        Map<Integer, JCheckBox> deviceBoxes = new LinkedHashMap<>();
        for (int device : deviceIds) {
            JCheckBox box = checkBox("Device " + device, presetDevices == null || presetDevices.contains(device));
            deviceBoxes.put(device, box);
            devicesRow.add(box);
        }
        right.add(devicesRow);
        right.add(Box.createVerticalStrut(12));
        right.add(sectionLabel("3. Pilih status"));
        JCheckBox includeYes = checkBox("Sudah WA", presetStatus == null || presetStatus.contains(true));
        JCheckBox includeNo = checkBox("Belum WA", presetStatus == null || presetStatus.contains(false));
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusRow.setOpaque(false); statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusRow.add(includeYes); statusRow.add(Box.createHorizontalStrut(12)); statusRow.add(includeNo);
        right.add(statusRow);
        right.add(Box.createVerticalStrut(12));
        right.add(sectionLabel("4. Format berkas"));
        JRadioButton asText = new JRadioButton("Notepad (.txt) — satu nomor per baris", true);
        JRadioButton asExcel = new JRadioButton("Excel (.xlsx) — tabel tanggal, device, COM, nomor, status");
        ButtonGroup formatGroup = new ButtonGroup();
        for (JRadioButton radio : new JRadioButton[]{asText, asExcel}) {
            radio.setOpaque(false); radio.setForeground(TEXT); radio.setFocusPainted(false);
            radio.setFont(new Font("SansSerif", Font.PLAIN, 13));
            radio.setAlignmentX(Component.LEFT_ALIGNMENT);
            formatGroup.add(radio); right.add(radio);
        }
        right.add(Box.createVerticalStrut(12));
        right.add(sectionLabel("5. Pengaturan nomor"));
        JPanel formatRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        formatRow.setOpaque(false); formatRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<String> numberFormat = new JComboBox<>(NUMBER_FORMATS);
        numberFormat.setPreferredSize(new Dimension(150, 30));
        formatRow.add(new JLabel("Awalan nomor:")); formatRow.add(numberFormat);
        right.add(formatRow);
        JCheckBox unique = checkBox("Buang nomor duplikat (nomor yang sama hanya ditulis sekali)", true);
        unique.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(unique);
        right.add(Box.createVerticalGlue());
        content.add(right, BorderLayout.CENTER);

        JLabel summary = new JLabel();
        summary.setFont(new Font("SansSerif", Font.BOLD, 13));
        summary.setForeground(ACCENT);
        JButton cancel = button("Batal", INPUT), save = button("Ekspor…", new Color(16, 159, 128));
        JPanel bottomRow = new JPanel(new BorderLayout(10, 0));
        bottomRow.setOpaque(false);
        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomButtons.setOpaque(false);
        bottomButtons.add(cancel); bottomButtons.add(save);
        bottomRow.add(summary, BorderLayout.CENTER);
        bottomRow.add(bottomButtons, BorderLayout.EAST);
        content.add(bottomRow, BorderLayout.SOUTH);

        java.util.function.Supplier<List<NumberRecord>> selection = () -> {
            Set<String> chosenDates = new HashSet<>();
            dateBoxes.forEach((date, box) -> { if (box.isSelected()) chosenDates.add(date); });
            Set<Integer> chosenDevices = new HashSet<>();
            deviceBoxes.forEach((device, box) -> { if (box.isSelected()) chosenDevices.add(device); });
            List<NumberRecord> chosen = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (NumberRecord record : records) {
                if (!chosenDates.contains(record.date()) || !chosenDevices.contains(record.device())) continue;
                if (record.sudah() ? !includeYes.isSelected() : !includeNo.isSelected()) continue;
                if (unique.isSelected() && !seen.add(record.number())) continue;
                chosen.add(record);
            }
            return chosen;
        };
        Runnable updateSummary = () -> {
            long chosenDates = dateBoxes.values().stream().filter(AbstractButton::isSelected).count();
            int total = selection.get().size();
            summary.setText(total + " nomor dari " + chosenDates + " tanggal akan diekspor");
            save.setEnabled(total > 0);
        };
        for (JCheckBox box : dateBoxes.values()) box.addItemListener(e -> updateSummary.run());
        for (JCheckBox box : deviceBoxes.values()) box.addItemListener(e -> updateSummary.run());
        for (JCheckBox box : new JCheckBox[]{includeYes, includeNo, unique}) box.addItemListener(e -> updateSummary.run());
        pickMonth.addActionListener(e -> {
            String month = months.get((String) monthPicker.getSelectedItem());
            if (month == null) return;
            dateBoxes.forEach((date, box) -> { if (date.startsWith(month)) box.setSelected(true); });
        });
        pickAll.addActionListener(e -> dateBoxes.values().forEach(box -> box.setSelected(true)));
        pickNone.addActionListener(e -> dateBoxes.values().forEach(box -> box.setSelected(false)));
        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> {
            List<NumberRecord> chosen = selection.get();
            if (chosen.isEmpty()) { error("Tidak ada nomor yang cocok dengan pilihan ekspor."); return; }
            boolean excel = asExcel.isSelected();
            String extension = excel ? ".xlsx" : ".txt";
            String first = chosen.get(0).date(), last = chosen.get(chosen.size() - 1).date();
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                excel ? "Excel (.xlsx)" : "Notepad (.txt)", excel ? "xlsx" : "txt"));
            chooser.setSelectedFile(new File("nomor-" + (first.equals(last) ? first : first + "_sd_" + last) + extension));
            if (chooser.showSaveDialog(dialog) != JFileChooser.APPROVE_OPTION) return;
            Path path = chooser.getSelectedFile().toPath();
            if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension))
                path = path.resolveSibling(path.getFileName() + extension);
            if (Files.exists(path) && JOptionPane.showConfirmDialog(dialog, "Timpa berkas yang sudah ada?", "Konfirmasi",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            int style = numberFormat.getSelectedIndex();
            try {
                if (excel) writeXlsx(path, chosen, style);
                else {
                    StringBuilder text = new StringBuilder();
                    for (NumberRecord record : chosen) text.append(formatNumber(record.number(), style)).append("\r\n");
                    Files.writeString(path, text.toString(), StandardCharsets.UTF_8);
                }
                dialog.dispose();
                JOptionPane.showMessageDialog(this, chosen.size() + " nomor berhasil diekspor ke:\n" + path.toAbsolutePath());
            } catch (IOException ex) { error("Gagal mengekspor: " + ex.getMessage()); }
        });

        styleTree(content);
        summary.setFont(new Font("SansSerif", Font.BOLD, 13));
        summary.setForeground(ACCENT);
        updateSummary.run();
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void textCell(StringBuilder sheet, String ref, String value, boolean header) {
        sheet.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"").append(header ? " s=\"1\"" : "")
            .append("><is><t>").append(xml(value)).append("</t></is></c>");
    }

    // Menulis .xlsx sederhana tanpa pustaka tambahan. Nomor disimpan sebagai teks agar awalan 0/+ tidak hilang.
    private static void writeXlsx(Path file, List<NumberRecord> rows, int style) throws IOException {
        String[] headers = {"No.", "Tanggal", "Device", "COM", "Nomor", "Status"};
        StringBuilder sheet = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>"
            + "<cols><col min=\"1\" max=\"1\" width=\"7\" customWidth=\"1\"/><col min=\"2\" max=\"2\" width=\"13\" customWidth=\"1\"/>"
            + "<col min=\"3\" max=\"3\" width=\"11\" customWidth=\"1\"/><col min=\"4\" max=\"4\" width=\"9\" customWidth=\"1\"/>"
            + "<col min=\"5\" max=\"5\" width=\"18\" customWidth=\"1\"/><col min=\"6\" max=\"6\" width=\"12\" customWidth=\"1\"/></cols><sheetData>");
        sheet.append("<row r=\"1\">");
        for (int column = 0; column < headers.length; column++) textCell(sheet, (char) ('A' + column) + "1", headers[column], true);
        sheet.append("</row>");
        int row = 2;
        for (NumberRecord record : rows) {
            sheet.append("<row r=\"").append(row).append("\"><c r=\"A").append(row).append("\"><v>").append(row - 1).append("</v></c>");
            textCell(sheet, "B" + row, record.date(), false);
            textCell(sheet, "C" + row, "Device " + record.device(), false);
            textCell(sheet, "D" + row, record.com(), false);
            textCell(sheet, "E" + row, formatNumber(record.number(), style), false);
            textCell(sheet, "F" + row, record.sudah() ? "Sudah WA" : "Belum WA", false);
            sheet.append("</row>");
            row++;
        }
        int lastRow = rows.size() + 1;
        sheet.append("</sheetData><autoFilter ref=\"A1:F").append(lastRow).append("\"/>")
            .append("<ignoredErrors><ignoredError sqref=\"E1:E").append(lastRow).append("\" numberStoredAsText=\"1\"/></ignoredErrors></worksheet>");
        Map<String, String> parts = new LinkedHashMap<>();
        parts.put("[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
        parts.put("_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        parts.put("xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets><sheet name=\"Nomor\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
            + "<definedNames><definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"0\" hidden=\"1\">Nomor!$A$1:$F$" + lastRow + "</definedName></definedNames></workbook>");
        parts.put("xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
        parts.put("xl/styles.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
            + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs></styleSheet>");
        parts.put("xl/worksheets/sheet1.xml", sheet.toString());
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(file))) {
            for (Map.Entry<String, String> part : parts.entrySet()) {
                zip.putNextEntry(new java.util.zip.ZipEntry(part.getKey()));
                zip.write(part.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    private void copyText(String value) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(value), null);
        count.setText("Device " + activeDevice + " • Disalin: " + value + "  •  Tanggal: " + activeDate);
    }

    private void copyAll() {
        Set<String> values = tabs.getSelectedIndex() == 0 ? yes : no;
        Set<String> hidden = tabs.getSelectedIndex() == 0 ? hiddenYes : hiddenNo;
        StringJoiner lines = new StringJoiner(System.lineSeparator());
        int copied = 0;
        for (String number : values) if (!hidden.contains(number)) { lines.add(display(number)); copied++; }
        if (copied == 0) { error("Tidak ada nomor yang terlihat pada riwayat device dan tanggal ini."); return; }
        copyText(lines.toString());
        count.setText(copied + " nomor Device " + activeDevice + " tanggal " + activeDate + " berhasil disalin.");
    }

    private void copySelectedFromDevice() {
        int index = tabs.getSelectedIndex();
        JTable table = tables[index];
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        for (int row : table.getSelectedRows())
            selected.add((String) models[index].getValueAt(table.convertRowIndexToModel(row), 2));
        if (selected.isEmpty()) { error("Pilih nomor pada tabel terlebih dahulu."); return; }
        copyText(String.join(System.lineSeparator(), selected));
        count.setText(selected.size() + " nomor terpilih dari Device " + activeDevice + " tanggal " + activeDate + " berhasil disalin.");
    }

    private JPanel createDatabasePage(boolean isYes) {
        int index = isYes ? 1 : 0;
        String status = isYes ? "Sudah WA" : "Belum WA";
        JPanel page = new RoundedPanel(new BorderLayout(12, 12));
        JPanel heading = new JPanel(new BorderLayout(0, 12));
        heading.setOpaque(false);
        JLabel title = new JLabel("Database " + status + " — Semua Device");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.add(title, BorderLayout.NORTH);
        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dateRow.setOpaque(false);
        dateRow.add(new JLabel("Pilih tanggal:"));
        databaseDates[index].setPreferredSize(new Dimension(145, 30));
        dateRow.add(databaseDates[index]);
        JButton calendarDate = button("Kalender", new Color(57, 113, 176));
        calendarDate.addActionListener(e -> {
            String selected = (String) databaseDates[index].getSelectedItem();
            showCalendar(calendarDate, selected == null ? today() : selected, date -> {
                boolean exists = false;
                for (int i = 0; i < databaseDates[index].getItemCount(); i++)
                    if (date.equals(databaseDates[index].getItemAt(i))) { exists = true; break; }
                if (!exists) databaseDates[index].addItem(date);
                databaseDates[index].setSelectedItem(date);
            });
        });
        dateRow.add(calendarDate);
        JButton inputOnDate = button("Input pada tanggal ini", new Color(57, 113, 176));
        inputOnDate.addActionListener(e -> {
            String selected = (String) databaseDates[index].getSelectedItem();
            if (selected == null) return;
            try { switchInputDate(selected); pageLayout.show(pages, "devices"); }
            catch (IOException ex) { error("Gagal membuka tanggal input: " + ex.getMessage()); }
        });
        dateRow.add(inputOnDate);
        dateRow.add(Box.createHorizontalStrut(12));
        dateRow.add(new JLabel("Cari nomor:"));
        databaseSearch[index].setToolTipText("Cari dengan awalan 8, 08, 62, atau +62");
        dateRow.add(databaseSearch[index]);
        heading.add(dateRow, BorderLayout.SOUTH);
        page.add(heading, BorderLayout.NORTH);

        databaseModels[index] = new DefaultTableModel(new Object[]{"Device", "COM", "Nomor"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(databaseModels[index]);
        databaseTables[index] = table;
        table.setRowHeight(31);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        styleTable(table);
        databaseSearch[index].getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterDatabase(index); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterDatabase(index); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterDatabase(index); }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    String number = (String) databaseModels[index].getValueAt(table.convertRowIndexToModel(row), 2);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(number), null);
                    databaseInfo[index].setText("Nomor disalin: " + number);
                }
            }
        });
        page.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton copySelected = button("Salin nomor terpilih", new Color(57, 113, 176));
        JButton copyAll = button("Salin semua nomor tanggal ini", new Color(31, 145, 111));
        JButton deleteSelected = button("Hapus terpilih", new Color(171, 65, 77));
        JButton deleteDate = button("Hapus semua pada tanggal ini", new Color(130, 55, 70));
        copySelected.addActionListener(e -> copyDatabaseNumbers(index, true));
        copyAll.addActionListener(e -> copyDatabaseNumbers(index, false));
        deleteSelected.addActionListener(e -> deleteDatabaseRecords(index, false));
        deleteDate.addActionListener(e -> deleteDatabaseRecords(index, true));
        JButton exportDate = button("Ekspor TXT/Excel", new Color(85, 93, 110));
        exportDate.addActionListener(e -> {
            String selected = (String) databaseDates[index].getSelectedItem();
            showExportDialog(Set.of(selected == null ? activeDate : selected), null, Set.of(isYes));
        });
        actions.add(copySelected);
        actions.add(copyAll);
        actions.add(exportDate);
        footer.add(actions, BorderLayout.NORTH);
        JPanel deleteActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        deleteActions.setOpaque(false);
        deleteActions.add(deleteSelected);
        deleteActions.add(deleteDate);
        footer.add(deleteActions, BorderLayout.CENTER);
        footer.add(databaseInfo[index], BorderLayout.SOUTH);
        page.add(footer, BorderLayout.SOUTH);
        populateDatabaseDates(index);
        databaseDates[index].addActionListener(e -> {
            if (!updatingDatabaseDates) refreshDatabasePage(index);
        });
        return page;
    }

    private void populateDatabaseDates(int index) {
        TreeSet<String> dates = new TreeSet<>(Comparator.reverseOrder());
        dates.add(today());
        try (DirectoryStream<Path> folders = Files.newDirectoryStream(directory)) {
            for (Path folder : folders) {
                if (!Files.isDirectory(folder)) continue;
                String name = folder.getFileName().toString();
                try { LocalDate.parse(name); dates.add(name); }
                catch (java.time.format.DateTimeParseException ignored) { }
            }
        } catch (IOException ex) { error("Gagal membaca tanggal database: " + ex.getMessage()); }
        String selected = (String) databaseDates[index].getSelectedItem();
        if (selected != null) dates.add(selected);
        updatingDatabaseDates = true;
        try {
            databaseDates[index].removeAllItems();
            for (String date : dates) databaseDates[index].addItem(date);
            databaseDates[index].setSelectedItem(selected != null ? selected : activeDate);
        } finally { updatingDatabaseDates = false; }
    }

    private void showDatabase(boolean isYes) {
        int index = isYes ? 1 : 0;
        populateDatabaseDates(index);
        refreshDatabasePage(index);
        pageLayout.show(pages, isYes ? "sudah" : "belum");
    }

    private void refreshDatabasePage(int index) {
        String date = (String) databaseDates[index].getSelectedItem();
        if (date == null) return;
        Path file = directory.resolve(date).resolve(index == 1 ? "sudah-wa.db" : "belum-wa.db");
        Map<Integer, LinkedHashSet<String>> all = new TreeMap<>();
        Map<Integer, Map<String, String>> allCom = new TreeMap<>();
        try { readShared(file, all, allCom); }
        catch (IOException ex) { error("Gagal membaca database: " + ex.getMessage()); return; }
        DefaultTableModel model = databaseModels[index];
        model.setRowCount(0);
        for (Map.Entry<Integer, LinkedHashSet<String>> group : all.entrySet())
            for (String number : group.getValue())
                model.addRow(new Object[]{"Device " + group.getKey(),
                    allCom.getOrDefault(group.getKey(), Collections.emptyMap()).getOrDefault(number, "—"), display(number)});
        filterDatabase(index);
    }

    private void filterDatabase(int index) {
        JTable table = databaseTables[index];
        if (table == null) return;
        String query = databaseSearch[index].getText().replaceAll("[^0-9]", "");
        if (query.startsWith("62")) query = query.substring(2);
        else if (query.startsWith("0")) query = query.substring(1);
        final String numberQuery = query;
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        sorter.setRowFilter(numberQuery.isEmpty() ? null : new RowFilter<DefaultTableModel, Integer>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                return entry.getStringValue(2).contains(numberQuery);
            }
        });
        String date = (String) databaseDates[index].getSelectedItem();
        if (date != null) databaseInfo[index].setText(date + " • Menampilkan " + table.getRowCount()
            + " dari " + databaseModels[index].getRowCount() + " catatan. Klik nomor untuk menyalinnya.");
    }

    private void copyDatabaseNumbers(int index, boolean selectedOnly) {
        JTable table = databaseTables[index];
        DefaultTableModel model = databaseModels[index];
        LinkedHashSet<String> numbers = new LinkedHashSet<>();
        if (selectedOnly) {
            for (int row : table.getSelectedRows())
                numbers.add((String) model.getValueAt(table.convertRowIndexToModel(row), 2));
        } else {
            for (int row = 0; row < model.getRowCount(); row++)
                numbers.add((String) model.getValueAt(row, 2));
        }
        if (numbers.isEmpty()) { error(selectedOnly ? "Pilih nomor pada tabel terlebih dahulu." : "Belum ada nomor pada tanggal ini."); return; }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
            new java.awt.datatransfer.StringSelection(String.join(System.lineSeparator(), numbers)), null);
        databaseInfo[index].setText(numbers.size() + " nomor unik dari " + databaseDates[index].getSelectedItem() + " berhasil disalin.");
    }

    private void deleteDatabaseRecords(int index, boolean entireDate) {
        String date = (String) databaseDates[index].getSelectedItem();
        if (date == null) return;
        JTable table = databaseTables[index];
        Map<Integer, Set<String>> selected = new TreeMap<>();
        if (!entireDate) {
            for (int row : table.getSelectedRows()) {
                int modelRow = table.convertRowIndexToModel(row);
                String deviceLabel = (String) databaseModels[index].getValueAt(modelRow, 0);
                int device = Integer.parseInt(deviceLabel.substring("Device ".length()));
                String number = normalize((String) databaseModels[index].getValueAt(modelRow, 2));
                if (number != null) selected.computeIfAbsent(device, ignored -> new LinkedHashSet<>()).add(number);
            }
        }
        int numberOfRows = entireDate ? databaseModels[index].getRowCount() : selected.values().stream().mapToInt(Set::size).sum();
        if (numberOfRows == 0) { error(entireDate ? "Tanggal ini belum memiliki nomor." : "Pilih nomor dari tabel terlebih dahulu."); return; }
        String status = index == 1 ? "Sudah WA" : "Belum WA";
        String message = "Hapus " + numberOfRows + " catatan " + status + " tanggal " + date + " dari database utama?\n"
            + "Nomor yang dihapus tidak dapat dipulihkan dengan tombol Pulihkan riwayat.";
        if (JOptionPane.showConfirmDialog(this, message, "Konfirmasi penghapusan database", JOptionPane.YES_NO_OPTION,
                                          JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        Path file = directory.resolve(date).resolve(index == 1 ? "sudah-wa.db" : "belum-wa.db");
        try {
            int deleted = deleteShared(file, selected, entireDate);
            refreshDatabasePage(index);
            if (date.equals(activeDate)) { openDate(activeDate); refresh(); }
            databaseInfo[index].setText(deleted + " catatan " + status + " tanggal " + date + " dihapus dari database utama.");
        } catch (IOException ex) { error("Gagal menghapus dari database: " + ex.getMessage()); }
    }

    private void filter() {
        String q = search.getText().trim();
        sorters[tabs.getSelectedIndex()].setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(q), 1, 2));
    }

    private static String display(String number) {
        return number;
    }

    private void refresh() {
        Set<String>[] groups = new Set[]{yes, no};
        Set<String>[] hidden = new Set[]{hiddenYes, hiddenNo};
        Map<String, String>[] coms = new Map[]{yesCom, noCom};
        for (int i = 0; i < 2; i++) {
            models[i].setRowCount(0);
            int n = 1;
            for (String number : groups[i]) if (!hidden[i].contains(number))
                models[i].addRow(new Object[]{n++, coms[i].getOrDefault(number, "—"), display(number)});
            tabs.setTitleAt(i, (i == 0 ? "Sudah WA" : "Belum WA") + " (" + models[i].getRowCount() + ")");
        }
        updateCount();
    }

    private void updateCount() { count.setText("Device " + activeDevice + "  •  " + activeDate + "  •  Riwayat terlihat: " + (models[0].getRowCount() + models[1].getRowCount()) + " nomor  •  Sudah WA: " + models[0].getRowCount() + "  •  Belum WA: " + models[1].getRowCount()); }
    private void error(String message) { JOptionPane.showMessageDialog(this, message, "Nomor Sorter", JOptionPane.WARNING_MESSAGE); }

    // ===== Pembaruan otomatis dari GitHub Releases =====

    private record Release(String version, String zipUrl, String notes) { }

    static int compareVersions(String a, String b) {
        String[] left = a.replaceFirst("^[vV]", "").split("\\."), right = b.replaceFirst("^[vV]", "").split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int x = i < left.length ? parseVersionPart(left[i]) : 0, y = i < right.length ? parseVersionPart(right[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    private static int parseVersionPart(String part) {
        String digits = part.replaceAll("[^0-9].*$", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private static String jsonString(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        if (!matcher.find()) return null;
        String raw = matcher.group(1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\' || i + 1 >= raw.length()) { out.append(c); continue; }
            char next = raw.charAt(++i);
            switch (next) {
                case 'n' -> out.append('\n');
                case 'r' -> { }
                case 't' -> out.append('\t');
                case 'u' -> { if (i + 4 < raw.length()) { out.append((char) Integer.parseInt(raw.substring(i + 1, i + 5), 16)); i += 4; } }
                default -> out.append(next);
            }
        }
        return out.toString();
    }

    private static Release fetchLatestRelease() throws IOException, InterruptedException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(
                java.net.URI.create("https://api.github.com/repos/" + UPDATE_REPO + "/releases/latest"))
            .timeout(java.time.Duration.ofSeconds(15))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "NomorSorter/" + APP_VERSION).build();
        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) return null;
        if (response.statusCode() != 200) throw new IOException("GitHub menjawab kode " + response.statusCode());
        String json = response.body();
        String tag = jsonString(json, "tag_name");
        Matcher asset = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.zip)\"").matcher(json);
        if (tag == null || !asset.find()) return null;
        String notes = jsonString(json, "body");
        return new Release(tag.replaceFirst("^[vV]", ""), asset.group(1), notes == null ? "" : notes.trim());
    }

    private Path skippedVersionFile() { return directory.resolve("lewati-versi.txt"); }

    private void checkForUpdates(boolean manual) {
        new SwingWorker<Release, Void>() {
            @Override protected Release doInBackground() throws Exception { return fetchLatestRelease(); }
            @Override protected void done() {
                Release release;
                try { release = get(); }
                catch (Exception ex) {
                    if (manual) error("Tidak dapat memeriksa pembaruan. Pastikan komputer terhubung ke internet.\n"
                        + (ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage()));
                    return;
                }
                if (release == null || compareVersions(release.version(), APP_VERSION) <= 0) {
                    if (manual) JOptionPane.showMessageDialog(NomorSorter.this, "Nomor Sorter sudah versi terbaru (" + APP_VERSION + ").");
                    return;
                }
                if (!manual) {
                    try {
                        Path skipped = skippedVersionFile();
                        if (Files.exists(skipped) && Files.readString(skipped, StandardCharsets.UTF_8).trim().equals(release.version())) return;
                    } catch (IOException ignored) { }
                }
                promptUpdate(release);
            }
        }.execute();
    }

    private void promptUpdate(Release release) {
        JPanel message = new JPanel(new BorderLayout(0, 10));
        message.add(new JLabel("<html><b>Versi " + release.version() + " tersedia.</b> Versi terpasang: " + APP_VERSION
            + "<br>Data nomor tetap aman. Aplikasi akan ditutup sebentar lalu dibuka kembali.</html>"), BorderLayout.NORTH);
        if (!release.notes().isEmpty()) {
            JTextArea notes = new JTextArea(release.notes(), 8, 46);
            notes.setEditable(false); notes.setLineWrap(true); notes.setWrapStyleWord(true);
            message.add(new JScrollPane(notes), BorderLayout.CENTER);
        }
        String[] options = {"Perbarui sekarang", "Nanti", "Lewati versi ini"};
        int choice = JOptionPane.showOptionDialog(this, message, "Pembaruan Nomor Sorter", JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (choice == 2) {
            try { Files.writeString(skippedVersionFile(), release.version() + "\n", StandardCharsets.UTF_8); }
            catch (IOException ex) { error("Gagal menyimpan pilihan: " + ex.getMessage()); }
        }
        if (choice == 0) downloadAndInstall(release);
    }

    private static Path installationFolder() {
        try {
            Path location = Paths.get(NomorSorter.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(location) && location.getFileName().toString().equalsIgnoreCase("NomorSorter.jar"))
                return location.getParent();
        } catch (Exception ignored) { }
        return null;
    }

    private static String javaw() {
        Path bin = Paths.get(System.getProperty("java.home"), "bin");
        Path windowed = bin.resolve("javaw.exe");
        return Files.isRegularFile(windowed) ? windowed.toString() : bin.resolve("java").toString();
    }

    private void downloadAndInstall(Release release) {
        Path install = installationFolder();
        if (install == null || !Files.isRegularFile(install.resolve("NomorSorterUpdater.jar"))) {
            error("Pembaruan otomatis hanya bisa dijalankan dari folder NomorSorter yang berisi NomorSorter.jar dan NomorSorterUpdater.jar.");
            return;
        }
        boolean unsaved = !input.getText().isBlank() || drafts.stream().anyMatch(draft -> draft != null && !draft.isBlank());
        if (unsaved && JOptionPane.showConfirmDialog(this, "Masih ada teks input yang belum diproses dan akan hilang saat aplikasi dibuka ulang.\nLanjutkan pembaruan?",
                "Pembaruan", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        JDialog progress = new JDialog(this, "Mengunduh pembaruan", false);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setString("Mengunduh versi " + release.version() + "…");
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        body.add(bar);
        progress.setContentPane(body);
        progress.setSize(380, 100);
        progress.setLocationRelativeTo(this);
        progress.setVisible(true);
        new SwingWorker<Path, Integer>() {
            @Override protected Path doInBackground() throws Exception {
                Path staging = Files.createTempDirectory("nomorsorter-pembaruan-");
                Path zip = staging.resolve("NomorSorter_" + release.version() + ".zip");
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build();
                java.net.http.HttpResponse<InputStream> response = client.send(
                    java.net.http.HttpRequest.newBuilder(java.net.URI.create(release.zipUrl()))
                        .header("User-Agent", "NomorSorter/" + APP_VERSION).build(),
                    java.net.http.HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) throw new IOException("Unduhan gagal, kode " + response.statusCode());
                long total = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(zip)) {
                    byte[] buffer = new byte[16_384];
                    long done = 0;
                    for (int read; (read = in.read(buffer)) != -1; ) {
                        out.write(buffer, 0, read);
                        done += read;
                        if (total > 0) publish((int) (done * 100 / total));
                    }
                }
                // Salin pemasang ke folder sementara agar JAR di folder aplikasi tidak terkunci saat diganti.
                Files.copy(install.resolve("NomorSorter.jar"), staging.resolve("pemasang.jar"));
                Files.copy(install.resolve("NomorSorterUpdater.jar"), staging.resolve("NomorSorterUpdater.jar"));
                return zip;
            }
            @Override protected void process(List<Integer> chunks) {
                int value = chunks.get(chunks.size() - 1);
                bar.setValue(value);
                bar.setString("Mengunduh versi " + release.version() + "… " + value + "%");
            }
            @Override protected void done() {
                progress.dispose();
                Path zip;
                try { zip = get(); }
                catch (Exception ex) {
                    error("Gagal mengunduh pembaruan: " + (ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage()));
                    return;
                }
                try {
                    new ProcessBuilder(javaw(), "-cp", zip.resolveSibling("pemasang.jar").toString(), "NomorSorter",
                        "--pasang-pembaruan", Long.toString(ProcessHandle.current().pid()), zip.toString(), install.toString())
                        .directory(install.toFile()).start();
                } catch (IOException ex) { error("Gagal menjalankan pemasang pembaruan: " + ex.getMessage()); return; }
                System.exit(0);
            }
        }.execute();
    }

    private void reportFailedUpdate() {
        Path failed = directory.resolve("pembaruan-gagal.txt");
        if (!Files.exists(failed)) return;
        try {
            String log = Files.readString(failed, StandardCharsets.UTF_8);
            Files.delete(failed);
            error("Pembaruan terakhir gagal dipasang, aplikasi tetap memakai versi " + APP_VERSION + ".\n\n" + log.trim());
        } catch (IOException ignored) { }
    }

    /** Dijalankan dari salinan sementara: tunggu aplikasi tertutup, pasang ZIP lewat updater, lalu buka ulang. */
    private static void installUpdate(long pid, Path zip, Path install) {
        JWindow window = new JWindow();
        JLabel label = new JLabel("Memasang pembaruan Nomor Sorter…", SwingConstants.CENTER);
        label.setOpaque(true); label.setBackground(PANEL); label.setForeground(TEXT);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setBorder(BorderFactory.createLineBorder(BORDER));
        window.add(label);
        window.setSize(360, 80);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        try {
            ProcessHandle.of(pid).ifPresent(handle -> {
                try { handle.onExit().get(60, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception ignored) { }
            });
            Thread.sleep(500);
            Path log = zip.resolveSibling("pembaruan.log");
            Process updater = new ProcessBuilder(javaw(), "-jar", zip.resolveSibling("NomorSorterUpdater.jar").toString(),
                    "--apply", zip.toString(), install.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if (updater.waitFor() != 0) {
                Path data = install.resolve("data");
                Files.createDirectories(data);
                Files.writeString(data.resolve("pembaruan-gagal.txt"), Files.readString(log, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            }
            new ProcessBuilder(javaw(), "-jar", install.resolve("NomorSorter.jar").toString()).directory(install.toFile()).start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Pembaruan gagal: " + ex.getMessage() + "\nBuka ulang Nomor Sorter lewat jalankan.bat.");
        } finally {
            window.dispose();
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        if (args.length == 4 && args[0].equals("--pasang-pembaruan")) {
            installUpdate(Long.parseLong(args[1]), Paths.get(args[2]), Paths.get(args[3]));
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try { new NomorSorter().setVisible(true); }
            catch (IOException ex) { JOptionPane.showMessageDialog(null, "Gagal membuka basis data: " + ex.getMessage()); }
        });
    }
}
