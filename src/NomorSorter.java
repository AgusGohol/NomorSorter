import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
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
    static final String APP_VERSION = "2.11.0";
    private static java.nio.channels.FileLock appLock;
    private static final String UPDATE_REPO = "AgusGohol/NomorSorter";
    // Palet: satu warna aksen (hijau) di atas permukaan biru gelap netral.
    private static final Color BACKGROUND = new Color(11, 18, 32);
    private static final Color SIDEBAR = new Color(14, 22, 38);
    private static final Color PANEL = new Color(16, 25, 42);
    private static final Color INPUT = new Color(11, 18, 32);
    private static final Color HOVER = new Color(26, 39, 61);
    private static final Color ROW_LINE = new Color(24, 35, 54);
    private static final Color ROW_SELECTED = new Color(22, 52, 76);
    private static final Color TEXT = new Color(230, 237, 246);
    private static final Color MUTED = new Color(150, 164, 186);
    private static final Color SUBTLE = new Color(100, 117, 142);
    private static final Color ACCENT = new Color(47, 191, 143);
    private static final Color ON_ACCENT = new Color(6, 38, 27);
    private static final Color BORDER = new Color(31, 44, 65);
    private static final Color BORDER_STRONG = new Color(42, 58, 84);
    private static final Color DANGER = new Color(240, 104, 110);
    private static final String FONT_NAME = availableFont("Segoe UI", "SansSerif");
    private static final String FONT_STRONG = availableFont("Segoe UI Semibold", FONT_NAME);
    private static final String FONT_MONO = availableFont("Consolas", "Monospaced");
    private static final String ICON_FONT = availableFont("Segoe Fluent Icons", availableFont("Segoe MDL2 Assets", null));
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
    private final JTextArea input = new HintTextArea("Tempel baris data, contoh:  COM7  XL Axiata  083172103663  -51  on  69  1 | wa", 5, 42);
    private final JTextField search = new HintTextField("Cari nomor", 14);
    private final JLabel count = new JLabel();
    private final JLabel pageSubtitle = new JLabel();
    private final JToggleButton[] segments = new JToggleButton[2];
    private JToggleButton navDevices;
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
    private final JTextField[] databaseSearch = {new HintTextField("Cari nomor (8, 08, 62, +62)", 16), new HintTextField("Cari nomor (8, 08, 62, +62)", 16)};
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
        dailyBackup();
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
        writeAllTextFiles();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setSize(1240, 800);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        setContentPane(root);

        // ----- Sisi kiri: nama aplikasi, navigasi, pembaruan -----
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(212, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER),
            BorderFactory.createEmptyBorder(20, 12, 14, 12)));
        JPanel brand = row(10, new LogoBadge(), label("Nomor Sorter", true, 15, TEXT));
        brand.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        addToSidebar(sidebar, brand);
        sidebar.add(Box.createVerticalStrut(26));
        addToSidebar(sidebar, sidebarSection("Kerja"));
        JToggleButton showDevices = styled(new JToggleButton("Input device"), Kind.NAV, IC_DEVICE);
        navDevices = showDevices;
        addToSidebar(sidebar, showDevices);
        sidebar.add(Box.createVerticalStrut(16));
        addToSidebar(sidebar, sidebarSection("Database"));
        JToggleButton showNo = styled(new JToggleButton("Belum WA"), Kind.NAV, IC_PENDING);
        JToggleButton showYes = styled(new JToggleButton("Sudah WA"), Kind.NAV, IC_DONE);
        JButton exportNumbers = styled(new JButton("Ekspor nomor"), Kind.NAV, IC_EXPORT);
        exportNumbers.setToolTipText("Ekspor nomor dari beberapa tanggal dan device ke Notepad (.txt) atau Excel (.xlsx)");
        exportNumbers.addActionListener(e -> showExportDialog(Set.of(activeDate), null, null));
        addToSidebar(sidebar, showNo);
        sidebar.add(Box.createVerticalStrut(2));
        addToSidebar(sidebar, showYes);
        sidebar.add(Box.createVerticalStrut(2));
        addToSidebar(sidebar, exportNumbers);
        ButtonGroup menu = new ButtonGroup();
        for (JToggleButton nav : new JToggleButton[]{showDevices, showNo, showYes}) {
            menu.add(nav);
            nav.putClientProperty("accentIcon", Boolean.TRUE);
        }
        showDevices.setSelected(true);
        showNo.addActionListener(e -> showDatabase(false));
        showYes.addActionListener(e -> showDatabase(true));
        showDevices.addActionListener(e -> pageLayout.show(pages, "devices"));
        sidebar.add(Box.createVerticalGlue());
        JButton checkUpdate = styled(new JButton("Periksa pembaruan"), Kind.NAV, IC_SYNC);
        checkUpdate.addActionListener(e -> checkForUpdates(true));
        addToSidebar(sidebar, checkUpdate);
        JLabel versionLabel = label("Versi " + APP_VERSION, false, 11, SUBTLE);
        versionLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 0, 0));
        addToSidebar(sidebar, versionLabel);
        root.add(sidebar, BorderLayout.WEST);

        // ----- Halaman input device -----
        datePicker.setPreferredSize(new Dimension(140, 34));
        JButton manualDate = iconButton(IC_CALENDAR, "Pilih tanggal dari kalender");
        if (ICON_FONT == null) manualDate.setText("Kalender");
        manualDate.addActionListener(e -> showCalendar(manualDate, activeDate, date -> {
            try { switchInputDate(date); }
            catch (IOException ex) { error("Gagal membuka tanggal input: " + ex.getMessage()); }
        }));
        refreshDates();
        datePicker.addActionListener(e -> {
            String selected = (String) datePicker.getSelectedItem();
            if (!updatingDates && selected != null && !selected.equals(activeDate)) {
                try { switchInputDate(selected); }
                catch (IOException ex) { error("Gagal membuka tanggal: " + ex.getMessage()); datePicker.setSelectedItem(activeDate); }
            }
        });
        JPanel header = pageHeader("Input device", pageSubtitle, row(6, datePicker, manualDate));

        devicesPanel.setOpaque(false);
        devicesPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        rebuildDeviceButtons();

        input.setLineWrap(true); input.setWrapStyleWord(true);
        JScrollPane inputScroll = new JScrollPane(input);
        inputScroll.setBorder(BorderFactory.createEmptyBorder());
        inputScroll.getViewport().setBackground(PANEL);
        inputScroll.setPreferredSize(new Dimension(400, 104));
        JButton autoSort = primary("Sortir otomatis");
        autoSort.setToolTipText("Baris yang bertuliskan \"wa\" masuk Sudah WA, selebihnya masuk Belum WA");
        JButton addYes = secondary("Ke Sudah WA", (char) 0);
        JButton addNo = secondary("Ke Belum WA", (char) 0);
        JButton importFile = ghost("Impor file", IC_IMPORT);
        importFile.setToolTipText("Isi kotak input dari berkas TXT atau CSV");
        autoSort.addActionListener(e -> sortAutomatically());
        addYes.addActionListener(e -> add(input.getText(), true));
        addNo.addActionListener(e -> add(input.getText(), false));
        importFile.addActionListener(e -> importText());
        JPanel inputBar = new JPanel(new BorderLayout());
        inputBar.setOpaque(false);
        inputBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        inputBar.add(row(8, autoSort, addYes, addNo), BorderLayout.WEST);
        inputBar.add(importFile, BorderLayout.EAST);
        JPanel inputCard = card(new BorderLayout());
        inputCard.add(inputScroll, BorderLayout.CENTER);
        inputCard.add(inputBar, BorderLayout.SOUTH);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        for (JComponent part : new JComponent[]{header, devicesPanel, inputCard}) {
            part.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (north.getComponentCount() > 0) north.add(Box.createVerticalStrut(part == devicesPanel ? 16 : 14));
            north.add(part);
        }

        // Tabel Sudah WA / Belum WA. Tab bawaan disembunyikan; pilihan lewat tombol segmen.
        for (int i = 0; i < 2; i++) {
            models[i] = new DefaultTableModel(new Object[]{"No.", "COM", "Nomor telepon"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            tables[i] = new JTable(models[i]);
            styleTable(tables[i]);
            tables[i].getColumnModel().getColumn(0).setMaxWidth(70);
            tables[i].getColumnModel().getColumn(1).setMaxWidth(120);
            tables[i].setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            sorters[i] = new TableRowSorter<>(models[i]);
            tables[i].setRowSorter(sorters[i]);
            final int tableIndex = i;
            tables[i].addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    int row = tables[tableIndex].rowAtPoint(e.getPoint());
                    if (row >= 0) copyText((String) models[tableIndex].getValueAt(tables[tableIndex].convertRowIndexToModel(row), 2));
                }
            });
            tabs.addTab(i == 0 ? "Sudah WA" : "Belum WA", tableScroll(tables[i]));
        }
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override protected int calculateTabAreaHeight(int placement, int runs, int maxHeight) { return 0; }
            @Override protected Insets getContentBorderInsets(int placement) { return new Insets(0, 0, 0, 0); }
            @Override protected void paintTabArea(Graphics g, int placement, int selected) { }
            @Override protected void paintContentBorder(Graphics g, int placement, int selected) { }
        });
        tabs.setOpaque(false);
        JPanel segmentBar = card(new FlowLayout(FlowLayout.LEFT, 2, 0));
        segmentBar.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));
        ButtonGroup segmentGroup = new ButtonGroup();
        for (int i = 0; i < 2; i++) {
            final int index = i;
            segments[i] = styled(new JToggleButton(i == 0 ? "Sudah WA" : "Belum WA"), Kind.SEGMENT, (char) 0);
            segments[i].addActionListener(e -> tabs.setSelectedIndex(index));
            segmentGroup.add(segments[i]);
            segmentBar.add(segments[i]);
        }
        segments[0].setSelected(true);
        tabs.addChangeListener(e -> { segments[tabs.getSelectedIndex()].setSelected(true); filter(); updateCount(); });
        search.setPreferredSize(new Dimension(230, 34));
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });
        JPanel resultsBar = new JPanel(new BorderLayout());
        resultsBar.setOpaque(false);
        resultsBar.add(segmentBar, BorderLayout.WEST);
        resultsBar.add(search, BorderLayout.EAST);
        JPanel tableCard = card(new BorderLayout());
        tableCard.add(tabs, BorderLayout.CENTER);
        JPanel results = new JPanel(new BorderLayout(0, 10));
        results.setOpaque(false);
        results.add(resultsBar, BorderLayout.NORTH);
        results.add(tableCard, BorderLayout.CENTER);

        JButton copySelected = secondary("Salin terpilih", IC_COPY);
        JButton copyAll = secondary("Salin semua", (char) 0);
        copySelected.setToolTipText("Salin nomor pada baris yang dipilih");
        copyAll.setToolTipText("Salin semua nomor yang terlihat pada device dan tanggal ini");
        copySelected.addActionListener(e -> copySelectedFromDevice());
        copyAll.addActionListener(e -> copyAll());
        JPopupMenu deviceMenu = popupMenu(
            menuItem("Sembunyikan nomor terpilih", IC_HIDE, false, this::hideSelectedFromHistory),
            menuItem("Kosongkan riwayat tanggal ini", IC_CLEAR, false, this::clearHistory),
            menuItem("Pulihkan riwayat tanggal ini", IC_RESTORE, false, this::restoreHistory),
            null,
            menuItem("Ekspor TXT/Excel…", IC_EXPORT, false,
                () -> showExportDialog(Set.of(activeDate), Set.of(activeDevice), Set.of(tabs.getSelectedIndex() == 0))));
        count.setFont(font(false, 12));
        count.setForeground(SUBTLE);
        JPanel actionBar = new JPanel(new BorderLayout(12, 0));
        actionBar.setOpaque(false);
        actionBar.add(row(8, copySelected, copyAll, moreButton(deviceMenu), copyAllDevicesButton()), BorderLayout.WEST);
        actionBar.add(count, BorderLayout.EAST);

        JPanel devicePage = new JPanel(new BorderLayout(0, 14));
        devicePage.setOpaque(false);
        devicePage.add(north, BorderLayout.NORTH);
        devicePage.add(results, BorderLayout.CENTER);
        devicePage.add(actionBar, BorderLayout.SOUTH);
        pages.setOpaque(false);
        pages.setBorder(BorderFactory.createEmptyBorder(22, 28, 18, 28));
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

    private static void addToSidebar(JPanel sidebar, JComponent item) {
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, item.getPreferredSize().height));
        sidebar.add(item);
    }

    private static JLabel sidebarSection(String text) {
        JLabel section = label(text, false, 11, SUBTLE);
        section.setBorder(BorderFactory.createEmptyBorder(0, 10, 6, 0));
        return section;
    }

    private static class LogoBadge extends JComponent {
        LogoBadge() { setPreferredSize(new Dimension(28, 28)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D graphics = smooth(g);
            graphics.setColor(ACCENT);
            graphics.fillRoundRect(0, 0, 28, 28, 9, 9);
            graphics.setColor(ON_ACCENT);
            graphics.setFont(font(true, 12));
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString("NS", (28 - metrics.stringWidth("NS")) / 2, (28 - metrics.getHeight()) / 2 + metrics.getAscent());
            graphics.dispose();
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

    // ===== Komponen tampilan =====

    private static String availableFont(String wanted, String fallback) {
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
            if (family.equalsIgnoreCase(wanted)) return family;
        return fallback;
    }

    private static Font font(boolean strong, int size) { return new Font(strong ? FONT_STRONG : FONT_NAME, Font.PLAIN, size); }

    private static Graphics2D smooth(Graphics g) {
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        return graphics;
    }

    // Kode ikon Segoe MDL2 Assets / Segoe Fluent Icons (bawaan Windows 10/11).
    private static final char IC_DEVICE = '', IC_PENDING = '', IC_DONE = '', IC_EXPORT = '',
        IC_SYNC = '', IC_CALENDAR = '', IC_SEARCH = '', IC_COPY = '', IC_MORE = '',
        IC_ADD = '', IC_IMPORT = '', IC_CHEVRON_DOWN = '', IC_PREV = '', IC_NEXT = '',
        IC_HIDE = '', IC_CLEAR = '', IC_RESTORE = '', IC_DELETE = '', IC_INPUT = '';

    /** Ikon huruf; warnanya mengikuti teks komponen, atau aksen saat tombol navigasi terpilih. */
    private static class GlyphIcon implements Icon {
        private final char glyph; private final int size; private final Color color;
        GlyphIcon(char glyph, int size) { this(glyph, size, null); }
        GlyphIcon(char glyph, int size, Color color) { this.glyph = glyph; this.size = size; this.color = color; }
        @Override public int getIconWidth() { return ICON_FONT == null ? 0 : size + 2; }
        @Override public int getIconHeight() { return ICON_FONT == null ? 0 : size + 2; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            if (ICON_FONT == null) return;
            Graphics2D graphics = smooth(g);
            Color paint = color != null ? color : c.getForeground();
            if (c instanceof AbstractButton b && b.isSelected() && b.getClientProperty("accentIcon") != null) paint = ACCENT;
            graphics.setColor(paint);
            graphics.setFont(new Font(ICON_FONT, Font.PLAIN, size));
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString(String.valueOf(glyph), x + (getIconWidth() - metrics.charWidth(glyph)) / 2,
                y + (getIconHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            graphics.dispose();
        }
    }

    private enum Kind { PRIMARY, SECONDARY, GHOST, NAV, TAB, SEGMENT }

    /** Satu UI untuk semua tombol: warna dan bentuk ditentukan oleh jenisnya. */
    private static class FlatButtonUI extends BasicButtonUI {
        private final Kind kind;
        FlatButtonUI(Kind kind) { this.kind = kind; }
        @Override public void paint(Graphics g, JComponent component) {
            AbstractButton button = (AbstractButton) component;
            ButtonModel model = button.getModel();
            boolean hover = model.isRollover() && button.isEnabled(), pressed = model.isPressed();
            Graphics2D graphics = smooth(g);
            int w = component.getWidth(), h = component.getHeight();
            Color fill = null, stroke = null;
            switch (kind) {
                case PRIMARY -> fill = pressed ? ACCENT.darker() : hover ? new Color(66, 207, 160) : ACCENT;
                case SECONDARY -> { stroke = hover ? new Color(58, 77, 108) : BORDER_STRONG; if (hover || pressed) fill = HOVER; }
                case GHOST -> { if (hover || pressed || button.isSelected()) fill = HOVER; }
                case NAV -> { if (button.isSelected()) fill = HOVER; else if (hover) fill = new Color(20, 31, 50); }
                case SEGMENT -> { if (button.isSelected()) fill = HOVER; }
                case TAB -> { }
            }
            if (fill != null) { graphics.setColor(fill); graphics.fillRoundRect(0, 0, w - 1, h - 1, 8, 8); }
            if (stroke != null) { graphics.setColor(stroke); graphics.drawRoundRect(0, 0, w - 1, h - 1, 8, 8); }
            if (kind == Kind.TAB && button.isSelected()) { graphics.setColor(ACCENT); graphics.fillRect(6, h - 2, w - 12, 2); }
            graphics.dispose();
            Color base = button.getClientProperty("baseColor") instanceof Color c ? c : TEXT;
            if (kind == Kind.PRIMARY) button.setForeground(ON_ACCENT);
            else if (kind == Kind.TAB || kind == Kind.SEGMENT || kind == Kind.NAV)
                button.setForeground(button.isSelected() || hover ? TEXT : MUTED);
            else button.setForeground(base);
            super.paint(g, component);
        }
        @Override protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
            Graphics2D graphics = smooth(g);
            graphics.setFont(b.getFont());
            graphics.setColor(b.isEnabled() ? b.getForeground() : SUBTLE);
            graphics.drawString(text, textRect.x, textRect.y + graphics.getFontMetrics().getAscent());
            graphics.dispose();
        }
    }

    private static <B extends AbstractButton> B styled(B b, Kind kind, char glyph) {
        b.setUI(new FlatButtonUI(kind));
        b.setFont(font(kind == Kind.PRIMARY || kind == Kind.SECONDARY || kind == Kind.TAB, 13));
        b.setForeground(TEXT);
        b.setOpaque(false); b.setContentAreaFilled(false); b.setFocusPainted(false); b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (glyph != 0) { b.setIcon(new GlyphIcon(glyph, 14)); b.setIconTextGap(8); }
        switch (kind) {
            case NAV -> { b.setHorizontalAlignment(SwingConstants.LEFT); b.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10)); }
            case TAB -> b.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
            case SEGMENT -> b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            default -> b.setBorder(BorderFactory.createEmptyBorder(7, b.getText().isEmpty() ? 9 : 13, 7, b.getText().isEmpty() ? 9 : 13));
        }
        return b;
    }

    private static JButton primary(String label) { return styled(new JButton(label), Kind.PRIMARY, (char) 0); }
    private static JButton secondary(String label, char glyph) { return styled(new JButton(label), Kind.SECONDARY, glyph); }
    private static JButton ghost(String label, char glyph) { return styled(new JButton(label), Kind.GHOST, glyph); }
    private static JButton iconButton(char glyph, String tooltip) {
        JButton b = styled(new JButton(""), Kind.GHOST, glyph);
        b.setToolTipText(tooltip);
        return b;
    }

    /** Garis tepi membulat yang tipis, untuk kartu dan kolom isian. */
    private static class RoundBorder implements javax.swing.border.Border {
        private final Color color; private final Insets insets; private final int radius;
        RoundBorder(Color color, int radius, int top, int left, int bottom, int right) {
            this.color = color; this.radius = radius; insets = new Insets(top, left, bottom, right);
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D graphics = smooth(g);
            graphics.setColor(c.hasFocus() && c instanceof JTextComponent ? new Color(47, 191, 143, 150) : color);
            graphics.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            graphics.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return insets; }
        @Override public boolean isBorderOpaque() { return false; }
    }

    private static javax.swing.border.Border fieldBorder(int leftPadding) { return new RoundBorder(BORDER_STRONG, 8, 6, leftPadding, 6, 10); }

    private static class HintTextField extends JTextField {
        private final String hint;
        HintTextField(String hint, int columns) {
            super(columns); this.hint = hint;
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D graphics = smooth(g);
            graphics.setColor(getBackground());
            graphics.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            graphics.dispose();
            super.paintComponent(g);
            graphics = smooth(g);
            Insets insets = getInsets();
            if (ICON_FONT != null) new GlyphIcon(IC_SEARCH, 12, SUBTLE).paintIcon(this, graphics, 10, (getHeight() - 14) / 2);
            if (getText().isEmpty()) {
                graphics.setColor(SUBTLE);
                graphics.setFont(getFont());
                FontMetrics metrics = graphics.getFontMetrics();
                graphics.drawString(hint, insets.left, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            }
            graphics.dispose();
        }
    }

    private static class HintTextArea extends JTextArea {
        private final String hint;
        HintTextArea(String hint, int rows, int columns) { super(rows, columns); this.hint = hint; }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!getText().isEmpty()) return;
            Graphics2D graphics = smooth(g);
            graphics.setColor(SUBTLE);
            graphics.setFont(getFont());
            Insets insets = getInsets();
            graphics.drawString(hint, insets.left, insets.top + graphics.getFontMetrics().getAscent());
            graphics.dispose();
        }
    }

    /** Kotak centang & tombol pilihan yang digambar sendiri agar menyatu dengan tema gelap. */
    private static class ChoiceIcon implements Icon {
        private final boolean radio;
        ChoiceIcon(boolean radio) { this.radio = radio; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            boolean on = ((AbstractButton) c).isSelected();
            Graphics2D graphics = smooth(g);
            if (radio) {
                graphics.setColor(on ? ACCENT : BORDER_STRONG);
                graphics.setStroke(new BasicStroke(1.5f));
                graphics.drawOval(x + 1, y + 1, 13, 13);
                if (on) graphics.fillOval(x + 5, y + 5, 6, 6);
            } else {
                graphics.setColor(on ? ACCENT : INPUT);
                graphics.fillRoundRect(x + 1, y + 1, 14, 14, 5, 5);
                graphics.setColor(on ? ACCENT : BORDER_STRONG);
                graphics.drawRoundRect(x + 1, y + 1, 14, 14, 5, 5);
                if (on) {
                    graphics.setColor(ON_ACCENT);
                    graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    graphics.drawPolyline(new int[]{x + 4, x + 7, x + 12}, new int[]{y + 8, y + 11, y + 5}, 3);
                }
            }
            graphics.dispose();
        }
    }

    private static class ThinScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected void configureScrollBarColors() { trackColor = PANEL; thumbColor = BORDER_STRONG; }
        @Override protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return zeroButton(); }
        private static JButton zeroButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(c.getParent() != null ? c.getParent().getBackground() : PANEL);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D graphics = smooth(g);
            graphics.setColor(isThumbRollover() ? new Color(62, 82, 113) : thumbColor);
            graphics.fillRoundRect(r.x + 3, r.y + 3, r.width - 6, r.height - 6, 6, 6);
            graphics.dispose();
        }
    }

    private static class FlatComboUI extends javax.swing.plaf.basic.BasicComboBoxUI {
        @Override protected JButton createArrowButton() {
            JButton arrow = new JButton(new GlyphIcon(IC_CHEVRON_DOWN, 10, MUTED));
            arrow.setOpaque(false); arrow.setContentAreaFilled(false); arrow.setBorderPainted(false); arrow.setFocusPainted(false);
            if (ICON_FONT == null) arrow.setText("▾");
            arrow.setForeground(MUTED);
            return arrow;
        }
        @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) { }
        @Override public void paint(Graphics g, JComponent c) {
            Graphics2D graphics = smooth(g);
            graphics.setColor(INPUT);
            graphics.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 8, 8);
            graphics.dispose();
            super.paint(g, c);
        }
        @Override protected javax.swing.plaf.basic.ComboPopup createPopup() {
            javax.swing.plaf.basic.BasicComboPopup popup = new javax.swing.plaf.basic.BasicComboPopup(comboBox);
            popup.setBorder(BorderFactory.createLineBorder(BORDER_STRONG));
            return popup;
        }
    }

    private static JLabel label(String text, boolean strong, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(strong, size));
        label.setForeground(color);
        return label;
    }

    // Isi menu memakai tombol biasa agar tampilannya sama dengan bagian lain (menu bawaan Windows berbingkai putih).
    private static JButton menuItem(String text, char glyph, boolean danger, Runnable action) {
        JButton item = ghost(text, glyph);
        item.setHorizontalAlignment(SwingConstants.LEFT);
        item.setFont(font(false, 13));
        item.setIconTextGap(10);
        item.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 22));
        if (danger) item.putClientProperty("baseColor", DANGER);
        item.addActionListener(e -> action.run());
        return item;
    }

    private static JPopupMenu popupMenu(JButton... items) {
        JPopupMenu menu = new JPopupMenu();
        menu.setUI(new javax.swing.plaf.basic.BasicPopupMenuUI());
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(PANEL);
        menu.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_STRONG),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        for (JButton item : items) {
            if (item == null) {
                JPanel separator = new JPanel();
                separator.setBackground(BORDER);
                separator.setPreferredSize(new Dimension(1, 1));
                separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                menu.add(Box.createVerticalStrut(4)); menu.add(separator); menu.add(Box.createVerticalStrut(4));
            } else {
                item.addActionListener(e -> menu.setVisible(false));
                item.setAlignmentX(Component.LEFT_ALIGNMENT);
                Dimension size = item.getPreferredSize();
                item.setPreferredSize(new Dimension(size.width + 24, size.height));
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
                menu.add(item);
            }
        }
        return menu;
    }

    private static JButton moreButton(JPopupMenu menu) {
        JButton more = secondary("", IC_MORE);
        if (ICON_FONT == null) more.setText("⋯");
        more.setToolTipText("Tindakan lain");
        // Tombol ini selalu berada di baris bawah, jadi menu dibuka ke atas.
        more.addActionListener(e -> menu.show(more, 0, -menu.getPreferredSize().height - 4));
        return more;
    }

    /** Kartu: latar sedikit lebih terang dengan garis tepi tipis; isi dipotong mengikuti sudut membulat. */
    private static JPanel card(LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D graphics = smooth(g);
                graphics.setColor(getBackground());
                graphics.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                graphics.dispose();
            }
            @Override protected void paintChildren(Graphics g) {
                Graphics2D graphics = (Graphics2D) g.create();
                graphics.clip(new java.awt.geom.RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 11, 11));
                super.paintChildren(graphics);
                graphics.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D graphics = smooth(g);
                graphics.setColor(BORDER);
                graphics.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                graphics.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        return panel;
    }

    private static JPanel row(int gap, Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        for (int i = 0; i < components.length; i++) {
            if (i > 0) panel.add(Box.createHorizontalStrut(gap));
            panel.add(components[i]);
        }
        return panel;
    }

    private static JPanel pageHeader(String title, JLabel subtitle, Component right) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 0));
        titles.setOpaque(false);
        titles.add(label(title, true, 20, TEXT));
        subtitle.setFont(font(false, 12)); subtitle.setForeground(SUBTLE);
        titles.add(subtitle);
        header.add(titles, BorderLayout.WEST);
        if (right != null) {
            JPanel holder = new JPanel(new GridBagLayout());
            holder.setOpaque(false);
            holder.add(right);
            header.add(holder, BorderLayout.EAST);
        }
        return header;
    }

    private static String longDate(String date) {
        return LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")));
    }

    private void styleTable(JTable table) {
        table.setBackground(PANEL);
        table.setForeground(TEXT);
        table.setSelectionBackground(ROW_SELECTED);
        table.setSelectionForeground(TEXT);
        table.setGridColor(ROW_LINE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowHeight(34);
        table.setFont(font(false, 13));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setPreferredSize(new Dimension(10, 34));
        table.getTableHeader().setBackground(PANEL);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer((t, value, selected, focus, row, column) -> {
            JLabel label = label(String.valueOf(value), false, 12, SUBTLE);
            label.setOpaque(true);
            label.setBackground(PANEL);
            label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(0, 16, 0, 8)));
            return label;
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                       boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, false, row, column);
                setBackground(selected ? ROW_SELECTED : PANEL);
                String name = t.getColumnName(column);
                setForeground(name.startsWith("Nomor") ? TEXT : MUTED);
                setFont(font(false, 13));
                setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
                return this;
            }
        });
    }

    private JScrollPane tableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(PANEL);
        JPanel corner = new JPanel(); corner.setBackground(PANEL);
        scroll.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, corner);
        return scroll;
    }

    private void styleTree(Component component) {
        if (component instanceof JLabel label) {
            if (label.getForeground() instanceof javax.swing.plaf.UIResource) label.setForeground(TEXT);
            if (label.getFont() instanceof javax.swing.plaf.UIResource) label.setFont(font(false, 13));
        }
        if (component instanceof JTextArea area) {
            area.setBackground(PANEL); area.setForeground(TEXT); area.setCaretColor(ACCENT);
            area.setSelectionColor(ROW_SELECTED); area.setSelectedTextColor(TEXT);
            area.setFont(new Font(FONT_MONO, Font.PLAIN, 13));
            area.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        }
        if (component instanceof JTextField field) {
            field.setOpaque(false);
            field.setBackground(INPUT); field.setForeground(TEXT); field.setCaretColor(ACCENT);
            field.setSelectionColor(ROW_SELECTED); field.setSelectedTextColor(TEXT);
            field.setFont(font(false, 13));
            field.setBorder(fieldBorder(field instanceof HintTextField && ICON_FONT != null ? 30 : 10));
        }
        if (component instanceof JComboBox<?> combo) {
            combo.setUI(new FlatComboUI());
            combo.setOpaque(false);
            combo.setBackground(INPUT); combo.setForeground(TEXT);
            combo.setFont(font(false, 13));
            combo.setBorder(new RoundBorder(BORDER_STRONG, 8, 1, 6, 1, 2));
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                          boolean selected, boolean focused) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                    list.setBackground(PANEL);
                    label.setOpaque(index >= 0);
                    label.setBackground(selected ? HOVER : PANEL);
                    label.setForeground(TEXT);
                    label.setFont(font(false, 13));
                    label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                    return label;
                }
            });
        }
        if (component instanceof JCheckBox box) { box.setIcon(new ChoiceIcon(false)); box.setSelectedIcon(new ChoiceIcon(false)); }
        if (component instanceof JRadioButton radio) { radio.setIcon(new ChoiceIcon(true)); radio.setSelectedIcon(new ChoiceIcon(true)); }
        if (component instanceof JScrollPane scroll) {
            scroll.getVerticalScrollBar().setUI(new ThinScrollBarUI());
            scroll.getHorizontalScrollBar().setUI(new ThinScrollBarUI());
            scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
            scroll.getVerticalScrollBar().setUnitIncrement(16);
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
            // SYNC: isi berkas benar-benar ditulis ke disk sebelum menggantikan berkas lama.
            Files.write(temp, lines, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    private int deleteShared(Path file, Map<Integer, Set<String>> selected, boolean entireDate, String date, boolean sudah) throws IOException {
        Map<Integer, LinkedHashSet<String>> all = new TreeMap<>();
        Map<Integer, Map<String, String>> coms = new TreeMap<>();
        readShared(file, all, coms);
        List<String> journal = new ArrayList<>();
        for (Map.Entry<Integer, LinkedHashSet<String>> group : all.entrySet()) {
            Set<String> targets = selected.getOrDefault(group.getKey(), Collections.emptySet());
            Iterator<String> items = group.getValue().iterator();
            while (items.hasNext()) {
                String number = items.next();
                if (entireDate || targets.contains(number)) {
                    items.remove();
                    Map<String, String> deviceComs = coms.get(group.getKey());
                    String com = deviceComs == null ? null : deviceComs.remove(number);
                    journal.add(journalLine("HAPUS", date, group.getKey(), sudah, number, com));
                }
            }
        }
        if (journal.isEmpty()) return 0;
        appendJournal(journal);
        writeShared(file, all, coms);
        writeTextFiles(date);
        return journal.size();
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
            JToggleButton device = styled(new JToggleButton("Device " + number), Kind.TAB, (char) 0);
            device.setSelected(number == activeDevice);
            device.addActionListener(e -> switchDevice(deviceNumber));
            if (number == deviceCount && deviceCount > 1) {
                // Device terakhir dapat dihapus lewat klik kanan (hanya jika belum berisi data).
                device.setToolTipText("Klik kanan untuk menghapus device ini");
                JPopupMenu remove = popupMenu(menuItem("Hapus Device " + number, IC_DELETE, true, this::removeLastDevice));
                device.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mousePressed(java.awt.event.MouseEvent e) { maybeShow(e); }
                    @Override public void mouseReleased(java.awt.event.MouseEvent e) { maybeShow(e); }
                    private void maybeShow(java.awt.event.MouseEvent e) { if (e.isPopupTrigger()) remove.show(device, e.getX(), e.getY()); }
                });
            }
            deviceGroup.add(device);
            devicesPanel.add(device);
        }
        JButton addDevice = iconButton(IC_ADD, "Tambah device");
        if (ICON_FONT == null) addDevice.setText("+");
        addDevice.addActionListener(e -> addDevice());
        devicesPanel.add(addDevice);
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
        popup.setBorder(BorderFactory.createLineBorder(BORDER_STRONG));
        JPanel calendar = new JPanel(new BorderLayout(5, 10));
        calendar.setBackground(PANEL);
        calendar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        popup.add(calendar);
        YearMonth[] visibleMonth = {YearMonth.from(selected)};
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            calendar.removeAll();
            JPanel heading = new JPanel(new BorderLayout());
            heading.setOpaque(false);
            JLabel month = label(visibleMonth[0].atDay(1).format(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"))), true, 14, TEXT);
            month.setHorizontalAlignment(SwingConstants.CENTER);
            JButton previous = iconButton(IC_PREV, "Bulan sebelumnya"), next = iconButton(IC_NEXT, "Bulan berikutnya");
            if (ICON_FONT == null) { previous.setText("‹"); next.setText("›"); }
            previous.addActionListener(e -> { visibleMonth[0] = visibleMonth[0].minusMonths(1); render[0].run(); });
            next.addActionListener(e -> { visibleMonth[0] = visibleMonth[0].plusMonths(1); render[0].run(); });
            heading.add(previous, BorderLayout.WEST);
            heading.add(month, BorderLayout.CENTER);
            heading.add(next, BorderLayout.EAST);
            calendar.add(heading, BorderLayout.NORTH);

            JPanel days = new JPanel(new GridLayout(0, 7, 2, 2));
            days.setOpaque(false);
            for (String dayName : new String[]{"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"}) {
                JLabel name = label(dayName, false, 11, SUBTLE);
                name.setHorizontalAlignment(SwingConstants.CENTER);
                days.add(name);
            }
            int offset = visibleMonth[0].atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < offset; i++) days.add(new JLabel());
            LocalDate now = LocalDate.now();
            for (int day = 1; day <= visibleMonth[0].lengthOfMonth(); day++) {
                LocalDate choice = visibleMonth[0].atDay(day);
                JButton date = choice.equals(selected) ? primary(Integer.toString(day)) : ghost(Integer.toString(day), (char) 0);
                if (choice.equals(now) && !choice.equals(selected)) date.putClientProperty("baseColor", ACCENT);
                date.setFont(font(choice.equals(selected) || choice.equals(now), 13));
                date.setHorizontalAlignment(SwingConstants.CENTER);
                date.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                date.setPreferredSize(new Dimension(38, 32));
                date.addActionListener(e -> { popup.setVisible(false); onSelect.accept(choice.toString()); });
                days.add(date);
            }
            calendar.add(days, BorderLayout.CENTER);
            JButton todayButton = secondary("Hari ini", (char) 0);
            todayButton.addActionListener(e -> { popup.setVisible(false); onSelect.accept(today()); });
            calendar.add(todayButton, BorderLayout.SOUTH);
            popup.pack();
            calendar.revalidate();
            calendar.repaint();
        };
        render[0].run();
        popup.show(anchor, anchor.getWidth() - popup.getPreferredSize().width, anchor.getHeight() + 4);
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
            Files.write(temporary, converted, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
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
            Files.write(temp, lines, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    private void save() throws IOException {
        mergeStatus(yesFile, activeDevice, yes, yesCom, true);
        mergeStatus(noFile, activeDevice, no, noCom, true);
    }

    // ===== Penyimpanan aman =====
    // 1) Setiap perubahan membaca ulang berkas dari disk lalu hanya menambahkan perubahan baru,
    //    sehingga data yang ditulis jendela lain tidak tertimpa salinan lama di memori.
    // 2) Perubahan dicatat lebih dulu ke jurnal (data/jurnal/), yang hanya pernah ditambah, tidak pernah ditimpa.
    // 3) Berkas ditulis dengan SYNC agar benar-benar sampai ke disk walau PC mati mendadak.

    /** changes: nomor -> true (Sudah WA) / false (Belum WA), untuk device dan tanggal yang aktif. */
    private void applyChanges(LinkedHashMap<String, Boolean> changes, Map<String, String> coms, String action) throws IOException {
        Path folder = directory.resolve(activeDate);
        Files.createDirectories(folder);
        List<String> journal = new ArrayList<>();
        for (Map.Entry<String, Boolean> change : changes.entrySet())
            journal.add(journalLine(action, activeDate, activeDevice, change.getValue(), change.getKey(), coms.getOrDefault(change.getKey(), "")));
        appendJournal(journal);
        Map<Integer, LinkedHashSet<String>> allYes = new TreeMap<>(), allNo = new TreeMap<>();
        Map<Integer, Map<String, String>> allYesCom = new TreeMap<>(), allNoCom = new TreeMap<>();
        readShared(folder.resolve("sudah-wa.db"), allYes, allYesCom);
        readShared(folder.resolve("belum-wa.db"), allNo, allNoCom);
        LinkedHashSet<String> deviceYes = allYes.computeIfAbsent(activeDevice, ignored -> new LinkedHashSet<>());
        LinkedHashSet<String> deviceNo = allNo.computeIfAbsent(activeDevice, ignored -> new LinkedHashSet<>());
        Map<String, String> deviceYesCom = allYesCom.computeIfAbsent(activeDevice, ignored -> new LinkedHashMap<>());
        Map<String, String> deviceNoCom = allNoCom.computeIfAbsent(activeDevice, ignored -> new LinkedHashMap<>());
        for (Map.Entry<String, Boolean> change : changes.entrySet()) {
            String number = change.getKey();
            boolean sudah = change.getValue();
            (sudah ? deviceNo : deviceYes).remove(number);
            String prior = (sudah ? deviceNoCom : deviceYesCom).remove(number);
            (sudah ? deviceYes : deviceNo).add(number);
            Map<String, String> targetCom = sudah ? deviceYesCom : deviceNoCom;
            if (coms.get(number) != null && !coms.get(number).isEmpty()) targetCom.put(number, coms.get(number));
            else if (prior != null) targetCom.putIfAbsent(number, prior);
            hiddenYes.remove(number);
            hiddenNo.remove(number);
        }
        writeShared(folder.resolve("sudah-wa.db"), allYes, allYesCom);
        writeShared(folder.resolve("belum-wa.db"), allNo, allNoCom);
        writeTextFiles(activeDate);
        saveHidden();
        openDate(activeDate);
        refresh();
    }

    // ===== Salinan teks per tanggal: <folder aplikasi>\Sudah WA\sudah wa YYYY-MM-DD.txt dan Belum WA\… =====
    // Hanya salinan untuk dibaca; database asli tetap di folder data. Isi: nomor 8…, urut per device, termasuk yang disembunyikan.

    private Path textFolder(boolean sudah) { return directory.toAbsolutePath().getParent().resolve(sudah ? "Sudah WA" : "Belum WA"); }

    private void writeTextFiles(String date) {
        for (boolean sudah : new boolean[]{true, false}) {
            Path file = textFolder(sudah).resolve((sudah ? "sudah wa " : "belum wa ") + date + ".txt");
            try {
                Map<Integer, LinkedHashSet<String>> all = new TreeMap<>();
                readShared(directory.resolve(date).resolve(sudah ? "sudah-wa.db" : "belum-wa.db"), all, new TreeMap<>());
                StringBuilder content = new StringBuilder();
                for (LinkedHashSet<String> group : all.values()) for (String number : group) content.append(display(number)).append("\r\n");
                if (content.length() == 0) { Files.deleteIfExists(file); continue; }
                if (Files.exists(file) && Files.readString(file, StandardCharsets.UTF_8).contentEquals(content)) continue;
                Files.createDirectories(file.getParent());
                Files.writeString(file, content, StandardCharsets.UTF_8);
            } catch (IOException ex) { System.err.println("Gagal menulis " + file + ": " + ex.getMessage()); }
        }
    }

    /** Saat aplikasi dibuka: samakan salinan teks semua tanggal dengan database. */
    private void writeAllTextFiles() {
        try (DirectoryStream<Path> days = Files.newDirectoryStream(directory)) {
            for (Path day : days) {
                if (!Files.isDirectory(day)) continue;
                try { LocalDate.parse(day.getFileName().toString()); }
                catch (java.time.format.DateTimeParseException ignored) { continue; }
                writeTextFiles(day.getFileName().toString());
            }
        } catch (IOException ex) { System.err.println("Gagal membuat salinan teks: " + ex.getMessage()); }
    }

    private static String journalLine(String action, String date, int device, boolean sudah, String number, String com) {
        return java.time.LocalDateTime.now().withNano(0) + "\t" + action + "\t" + date + "\t" + device + "\t"
            + (sudah ? "sudah" : "belum") + "\t" + number + "\t" + (com == null ? "" : com);
    }

    private void appendJournal(List<String> lines) throws IOException {
        if (lines.isEmpty()) return;
        Path folder = directory.resolve("jurnal");
        Files.createDirectories(folder);
        Files.write(folder.resolve(YearMonth.now() + ".txt"), lines, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.SYNC);
    }

    /** Device memakai tanggal manual? Tanyakan dulu agar nomor tidak "hilang" karena tersimpan di tanggal lain. */
    private boolean confirmTargetDate() {
        if (activeDate.equals(today())) return true;
        Object[] options = {"Simpan ke " + activeDate, "Pakai hari ini", "Batal"};
        int choice = JOptionPane.showOptionDialog(this, "Device " + activeDevice + " sedang memakai tanggal " + longDate(activeDate)
                + ", bukan hari ini.\nNomor akan disimpan ke tanggal tersebut.", "Tanggal penyimpanan",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == 0) return true;
        if (choice != 1) return false;
        try { switchInputDate(today()); return true; }
        catch (IOException ex) { error("Gagal membuka tanggal hari ini: " + ex.getMessage()); return false; }
    }

    private void add(String text, boolean toYes) {
        LinkedHashSet<String> numbers = extract(text);
        if (numbers.isEmpty()) { error("Tidak ditemukan nomor Indonesia yang valid (contoh: 081234567890)."); return; }
        if (!updateDateAtMidnight() || !confirmTargetDate()) return;
        LinkedHashMap<String, Boolean> changes = new LinkedHashMap<>();
        Map<String, String> coms = new HashMap<>();
        for (String line : text.split("\\R")) {
            String com = comIn(line);
            for (String number : extract(line)) {
                changes.put(number, toYes);
                if (com != null) coms.put(number, com);
            }
        }
        try { applyChanges(changes, coms, "SIMPAN"); }
        catch (IOException ex) { error("Gagal menyimpan data: " + ex.getMessage() + "\nTeks input tidak dihapus, coba simpan lagi."); return; }
        input.setText("");
        tabs.setSelectedIndex(toYes ? 0 : 1);
        JOptionPane.showMessageDialog(this, numbers.size() + " nomor disimpan ke Device " + activeDevice + ", tanggal " + activeDate + ".");
    }

    private void sortAutomatically() {
        if (extract(input.getText()).isEmpty()) { error("Tidak ditemukan nomor. Format yang diterima: 831234567890, 0831234567890, atau 62831234567890."); return; }
        if (!updateDateAtMidnight() || !confirmTargetDate()) return;
        int found = 0, marked = 0, unmarked = 0;
        LinkedHashMap<String, Boolean> changes = new LinkedHashMap<>();
        Map<String, String> coms = new HashMap<>();
        for (String line : input.getText().split("\\R")) {
            LinkedHashSet<String> numbers = extract(line);
            String com = comIn(line);
            boolean hasWa = WA_TAG.matcher(line).find() && !NO_WA_TAG.matcher(line).find();
            for (String number : numbers) {
                found++;
                changes.put(number, hasWa);
                if (com != null) coms.put(number, com);
                if (hasWa) marked++; else unmarked++;
            }
        }
        try { applyChanges(changes, coms, "SIMPAN"); }
        catch (IOException ex) { error("Gagal menyimpan data: " + ex.getMessage() + "\nTeks input tidak dihapus, coba simpan lagi."); return; }
        input.setText("");
        JOptionPane.showMessageDialog(this, "Diproses " + found + " baris nomor: " + marked + " bertanda WA, " + unmarked + " tanpa WA.\n"
            + "Disimpan ke Device " + activeDevice + ", tanggal " + activeDate + ".");
    }

    /** Cadangan otomatis sekali sehari saat aplikasi dibuka: data/cadangan/YYYY-MM-DD.zip, disimpan 30 hari terakhir. */
    private void dailyBackup() {
        try {
            Path folder = directory.resolve("cadangan");
            Files.createDirectories(folder);
            Path target = folder.resolve(today() + ".zip");
            if (Files.exists(target)) return;
            Path temp = Files.createTempFile(folder, "cadangan-", ".tmp");
            try {
                try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(temp));
                     DirectoryStream<Path> days = Files.newDirectoryStream(directory)) {
                    for (Path day : days) {
                        if (!Files.isDirectory(day)) continue;
                        try { LocalDate.parse(day.getFileName().toString()); }
                        catch (java.time.format.DateTimeParseException ignored) { continue; }
                        for (String status : new String[]{"sudah-wa.db", "belum-wa.db"}) {
                            Path file = day.resolve(status);
                            if (!Files.isRegularFile(file)) continue;
                            zip.putNextEntry(new java.util.zip.ZipEntry(day.getFileName() + "/" + status));
                            Files.copy(file, zip);
                            zip.closeEntry();
                        }
                    }
                }
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temp); }
            List<Path> backups = new ArrayList<>();
            try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "????-??-??.zip")) { for (Path file : files) backups.add(file); }
            backups.sort(Comparator.comparing(Path::toString));
            for (int i = 0; i < backups.size() - 30; i++) Files.deleteIfExists(backups.get(i));
        } catch (IOException ex) { System.err.println("Cadangan harian gagal: " + ex.getMessage()); }
    }

    /** Bandingkan jurnal dengan database tanggal ini; nomor yang tercatat disimpan tetapi tidak ada lagi ditawarkan untuk dipulihkan. */
    private void restoreFromJournal(int index) {
        String date = (String) databaseDates[index].getSelectedItem();
        if (date == null) return;
        // Putar ulang jurnal: kunci "device\tnomor" -> status terakhir; HAPUS menghapus kunci.
        Map<String, String[]> expected = new LinkedHashMap<>();
        Path folder = directory.resolve("jurnal");
        try {
            if (Files.isDirectory(folder)) {
                List<Path> files = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.txt")) { for (Path file : stream) files.add(file); }
                files.sort(Comparator.comparing(Path::toString));
                for (Path file : files)
                    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                        String[] c = line.split("\t", -1);
                        if (c.length < 7 || !c[2].equals(date)) continue;
                        String key = c[3] + "\t" + c[5];
                        if (c[1].equals("HAPUS")) { String[] prior = expected.get(key); if (prior != null && prior[0].equals(c[4])) expected.remove(key); }
                        else expected.put(key, new String[]{c[4], c[6]});
                    }
            }
            Map<Integer, LinkedHashSet<String>> allYes = new TreeMap<>(), allNo = new TreeMap<>();
            readShared(directory.resolve(date).resolve("sudah-wa.db"), allYes, new TreeMap<>());
            readShared(directory.resolve(date).resolve("belum-wa.db"), allNo, new TreeMap<>());
            Map<Integer, LinkedHashMap<String, Boolean>> missing = new TreeMap<>();
            Map<String, String> coms = new HashMap<>();
            int total = 0;
            for (Map.Entry<String, String[]> entry : expected.entrySet()) {
                String[] key = entry.getKey().split("\t");
                int device = Integer.parseInt(key[0]);
                String number = key[1];
                boolean present = allYes.getOrDefault(device, new LinkedHashSet<>()).contains(number)
                    || allNo.getOrDefault(device, new LinkedHashSet<>()).contains(number);
                if (present) continue;
                missing.computeIfAbsent(device, ignored -> new LinkedHashMap<>()).put(number, entry.getValue()[0].equals("sudah"));
                if (!entry.getValue()[1].isEmpty()) coms.put(number, entry.getValue()[1]);
                total++;
            }
            if (total == 0) {
                JOptionPane.showMessageDialog(this, expected.isEmpty()
                    ? "Jurnal belum mencatat input untuk tanggal " + date + ".\n(Jurnal mulai dicatat sejak versi 2.9.0.)"
                    : "Tidak ada nomor yang hilang pada tanggal " + date + ". Semua nomor di jurnal masih ada di database.");
                return;
            }
            if (JOptionPane.showConfirmDialog(this, total + " nomor tercatat pernah disimpan pada " + longDate(date)
                    + " tetapi tidak ada di database.\nNomor yang Anda hapus sendiri lewat menu hapus tidak ikut dipulihkan.\n\nPulihkan sekarang?",
                    "Pulihkan dari jurnal", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            int previousDevice = activeDevice;
            String previousDate = activeDate;
            try {
                for (Map.Entry<Integer, LinkedHashMap<String, Boolean>> group : missing.entrySet()) {
                    activeDevice = group.getKey();
                    activeDate = date;
                    hiddenFolderLoad();
                    applyChanges(group.getValue(), coms, "PULIH");
                }
            } finally {
                activeDevice = previousDevice;
                openDate(previousDate);
                refresh();
            }
            refreshDatabasePage(index);
            databaseInfo[index].setText(total + " nomor dipulihkan dari jurnal.");
        } catch (IOException | RuntimeException ex) { error("Gagal memulihkan dari jurnal: " + ex.getMessage()); }
    }

    private void hiddenFolderLoad() throws IOException {
        Set<String> newHiddenYes = new LinkedHashSet<>(), newHiddenNo = new LinkedHashSet<>();
        read(hiddenFolder(activeDate).resolve("sudah-wa.db"), newHiddenYes, new LinkedHashMap<>());
        read(hiddenFolder(activeDate).resolve("belum-wa.db"), newHiddenNo, new LinkedHashMap<>());
        hiddenYes.clear(); hiddenYes.addAll(newHiddenYes);
        hiddenNo.clear(); hiddenNo.addAll(newHiddenNo);
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
        box.setFont(font(false, 13));
        box.setIconTextGap(9);
        box.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        return box;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = label(text, true, 13, TEXT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
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
        JPanel content = new JPanel(new BorderLayout(28, 18));
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));
        dialog.setContentPane(content);

        // Kolom kiri: daftar tanggal yang bisa dicentang beberapa sekaligus.
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        left.add(sectionLabel("Tanggal"), BorderLayout.NORTH);
        JPanel dateList = new JPanel();
        dateList.setLayout(new BoxLayout(dateList, BoxLayout.Y_AXIS));
        dateList.setBackground(PANEL);
        dateList.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        Map<String, JCheckBox> dateBoxes = new LinkedHashMap<>();
        for (String date : dates) {
            String day = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE", indonesia));
            JCheckBox box = checkBox(date + "  ·  " + day + "  ·  " + perDate.getOrDefault(date, 0) + " catatan", presetDates.contains(date));
            dateBoxes.put(date, box);
            dateList.add(box);
        }
        JScrollPane dateScroll = new JScrollPane(dateList);
        dateScroll.setBorder(BorderFactory.createEmptyBorder());
        dateScroll.getViewport().setBackground(PANEL);
        JPanel dateCard = card(new BorderLayout());
        dateCard.setPreferredSize(new Dimension(360, 320));
        dateCard.add(dateScroll);
        left.add(dateCard, BorderLayout.CENTER);
        JPanel quick = new JPanel(new GridLayout(2, 1, 0, 8));
        quick.setOpaque(false);
        JPanel monthRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        monthRow.setOpaque(false);
        Map<String, String> months = new LinkedHashMap<>();
        for (String date : dates) {
            String key = date.substring(0, 7);
            months.putIfAbsent(YearMonth.parse(key).atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", indonesia)), key);
        }
        JComboBox<String> monthPicker = new JComboBox<>(months.keySet().toArray(new String[0]));
        monthPicker.setPreferredSize(new Dimension(150, 34));
        JButton pickMonth = secondary("Centang bulan ini", (char) 0);
        monthRow.add(monthPicker); monthRow.add(pickMonth);
        JPanel allRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        allRow.setOpaque(false);
        JButton pickAll = ghost("Centang semua", (char) 0), pickNone = ghost("Kosongkan", (char) 0);
        allRow.add(pickAll); allRow.add(pickNone);
        quick.add(monthRow); quick.add(allRow);
        left.add(quick, BorderLayout.SOUTH);
        content.add(left, BorderLayout.WEST);

        // Kolom kanan: device, status, dan format berkas.
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.add(sectionLabel("Device"));
        JPanel devicesRow = new JPanel(new GridLayout(0, 3, 10, 0));
        devicesRow.setOpaque(false);
        devicesRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        Map<Integer, JCheckBox> deviceBoxes = new LinkedHashMap<>();
        for (int device : deviceIds) {
            JCheckBox box = checkBox("Device " + device, presetDevices == null || presetDevices.contains(device));
            deviceBoxes.put(device, box);
            devicesRow.add(box);
        }
        right.add(devicesRow);
        right.add(Box.createVerticalStrut(18));
        right.add(sectionLabel("Status"));
        JCheckBox includeYes = checkBox("Sudah WA", presetStatus == null || presetStatus.contains(true));
        JCheckBox includeNo = checkBox("Belum WA", presetStatus == null || presetStatus.contains(false));
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusRow.setOpaque(false); statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusRow.add(includeYes); statusRow.add(Box.createHorizontalStrut(20)); statusRow.add(includeNo);
        right.add(statusRow);
        right.add(Box.createVerticalStrut(18));
        right.add(sectionLabel("Format berkas"));
        JRadioButton asText = new JRadioButton("Notepad (.txt)  ·  satu nomor per baris", true);
        JRadioButton asExcel = new JRadioButton("Excel (.xlsx)  ·  tanggal, device, COM, nomor, status");
        ButtonGroup formatGroup = new ButtonGroup();
        for (JRadioButton radio : new JRadioButton[]{asText, asExcel}) {
            radio.setOpaque(false); radio.setForeground(TEXT); radio.setFocusPainted(false);
            radio.setFont(font(false, 13));
            radio.setIconTextGap(9);
            radio.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
            radio.setAlignmentX(Component.LEFT_ALIGNMENT);
            formatGroup.add(radio); right.add(radio);
        }
        right.add(Box.createVerticalStrut(18));
        right.add(sectionLabel("Nomor"));
        JPanel formatRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        formatRow.setOpaque(false); formatRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<String> numberFormat = new JComboBox<>(NUMBER_FORMATS);
        numberFormat.setPreferredSize(new Dimension(160, 34));
        formatRow.add(label("Awalan", false, 13, MUTED)); formatRow.add(Box.createHorizontalStrut(10)); formatRow.add(numberFormat);
        right.add(formatRow);
        JCheckBox unique = checkBox("Buang nomor duplikat (nomor yang sama hanya ditulis sekali)", true);
        unique.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(unique);
        right.add(Box.createVerticalGlue());
        content.add(right, BorderLayout.CENTER);

        JLabel summary = new JLabel();
        JButton cancel = secondary("Batal", (char) 0), save = primary("Ekspor…");
        JPanel bottomRow = new JPanel(new BorderLayout(10, 0));
        bottomRow.setOpaque(false);
        bottomRow.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
            BorderFactory.createEmptyBorder(14, 0, 0, 0)));
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
        summary.setFont(font(true, 13));
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

    // ----- SEMENTARA (2.10.0): tombol "Salin semua device". Untuk menghapusnya, hapus blok ini
    // dan panggilan copyAllDevicesButton() di baris tombol halaman Input device. -----
    private JButton copyAllDevicesButton() {
        JButton button = secondary("Salin semua device", IC_COPY);
        button.setToolTipText("Salin nomor dari semua device pada tanggal aktif, termasuk yang disembunyikan");
        JPopupMenu choice = popupMenu(
            menuItem("Sudah WA", IC_DONE, false, () -> copyAllDevices(true)),
            menuItem("Belum WA", IC_PENDING, false, () -> copyAllDevices(false)));
        button.addActionListener(e -> choice.show(button, 0, -choice.getPreferredSize().height - 4));
        return button;
    }

    /** Salin per device (Device 1 dulu, lalu Device 2, …); nomor yang sama di dua device ikut tersalin dua kali. */
    private void copyAllDevices(boolean sudah) {
        Map<Integer, LinkedHashSet<String>> all = new TreeMap<>();
        try { readShared(directory.resolve(activeDate).resolve(sudah ? "sudah-wa.db" : "belum-wa.db"), all, new TreeMap<>()); }
        catch (IOException ex) { error("Gagal membaca database: " + ex.getMessage()); return; }
        List<String> numbers = new ArrayList<>();
        for (LinkedHashSet<String> group : all.values()) for (String number : group) numbers.add(display(number));
        String status = sudah ? "Sudah WA" : "Belum WA";
        if (numbers.isEmpty()) { error("Belum ada nomor " + status + " pada tanggal " + activeDate + "."); return; }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
            new java.awt.datatransfer.StringSelection(String.join(System.lineSeparator(), numbers)), null);
        count.setText(numbers.size() + " nomor " + status + " dari " + all.size() + " device (" + activeDate + ") disalin");
    }
    // ----- akhir blok sementara -----

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
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setOpaque(false);
        databaseDates[index].setPreferredSize(new Dimension(140, 34));
        JButton calendarDate = iconButton(IC_CALENDAR, "Pilih tanggal dari kalender");
        if (ICON_FONT == null) calendarDate.setText("Kalender");
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
        JButton inputOnDate = secondary("Input pada tanggal ini", IC_INPUT);
        inputOnDate.setToolTipText("Buka halaman input device dengan tanggal ini");
        inputOnDate.addActionListener(e -> {
            String selected = (String) databaseDates[index].getSelectedItem();
            if (selected == null) return;
            try { switchInputDate(selected); pageLayout.show(pages, "devices"); navDevices.setSelected(true); }
            catch (IOException ex) { error("Gagal membuka tanggal input: " + ex.getMessage()); }
        });
        JPanel heading = pageHeader(status, new JLabel("Database bersama semua device"),
            row(6, databaseDates[index], calendarDate, Box.createHorizontalStrut(4), inputOnDate));
        page.add(heading, BorderLayout.NORTH);

        databaseModels[index] = new DefaultTableModel(new Object[]{"Device", "COM", "Nomor"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(databaseModels[index]);
        databaseTables[index] = table;
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(140);
        table.getColumnModel().getColumn(1).setMaxWidth(120);
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
        databaseSearch[index].setPreferredSize(new Dimension(250, 34));
        databaseInfo[index].setFont(font(false, 12));
        databaseInfo[index].setForeground(SUBTLE);
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.add(databaseInfo[index], BorderLayout.WEST);
        toolbar.add(databaseSearch[index], BorderLayout.EAST);
        JPanel tableCard = card(new BorderLayout());
        tableCard.add(tableScroll(table), BorderLayout.CENTER);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(toolbar, BorderLayout.NORTH);
        content.add(tableCard, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);

        JButton copySelected = secondary("Salin terpilih", IC_COPY);
        JButton copyAll = secondary("Salin semua", (char) 0);
        copyAll.setToolTipText("Salin semua nomor unik pada tanggal ini");
        JButton exportDate = secondary("Ekspor", IC_EXPORT);
        exportDate.setToolTipText("Ekspor ke Notepad (.txt) atau Excel (.xlsx)");
        copySelected.addActionListener(e -> copyDatabaseNumbers(index, true));
        copyAll.addActionListener(e -> copyDatabaseNumbers(index, false));
        exportDate.addActionListener(e -> {
            String selected = (String) databaseDates[index].getSelectedItem();
            showExportDialog(Set.of(selected == null ? activeDate : selected), null, Set.of(isYes));
        });
        JPopupMenu deleteMenu = popupMenu(
            menuItem("Pulihkan nomor yang hilang (dari jurnal)", IC_RESTORE, false, () -> restoreFromJournal(index)),
            null,
            menuItem("Hapus terpilih dari database", IC_DELETE, true, () -> deleteDatabaseRecords(index, false)),
            menuItem("Hapus semua pada tanggal ini", IC_CLEAR, true, () -> deleteDatabaseRecords(index, true)));
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(row(8, copySelected, copyAll, exportDate, moreButton(deleteMenu)), BorderLayout.WEST);
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
        if (date != null) databaseInfo[index].setText(longDate(date) + "  ·  " + table.getRowCount()
            + " dari " + databaseModels[index].getRowCount() + " catatan  ·  klik nomor untuk menyalin");
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
            int deleted = deleteShared(file, selected, entireDate, date, index == 1);
            refreshDatabasePage(index);
            if (date.equals(activeDate)) { openDate(activeDate); refresh(); }
            databaseInfo[index].setText(deleted + " catatan " + status + " tanggal " + date + " dihapus dari database utama.");
        } catch (IOException ex) { error("Gagal menghapus dari database: " + ex.getMessage()); }
    }

    private void filter() {
        String q = search.getText().trim();
        if (q.matches("[+0-9 ]+")) {
            // Pencarian nomor menerima awalan 8, 08, 62, atau +62 seperti di halaman database.
            q = q.replaceAll("[^0-9]", "");
            if (q.startsWith("62")) q = q.substring(2);
            else if (q.startsWith("0")) q = q.substring(1);
        }
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
            segments[i].setText((i == 0 ? "Sudah WA" : "Belum WA") + "   " + models[i].getRowCount());
        }
        updateCount();
    }

    private void updateCount() {
        count.setText("Device " + activeDevice + "  ·  " + (models[0].getRowCount() + models[1].getRowCount()) + " nomor terlihat");
        pageSubtitle.setText(longDate(activeDate) + (activeDate.equals(today()) ? "" : "  ·  tanggal pilihan"));
    }
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

    private static java.net.HttpURLConnection openConnection(String url) throws IOException {
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) java.net.URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "NomorSorter/" + APP_VERSION);
        return connection;
    }

    private static Release fetchLatestRelease() throws IOException {
        java.net.HttpURLConnection connection = openConnection("https://api.github.com/repos/" + UPDATE_REPO + "/releases/latest");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        int status = connection.getResponseCode();
        if (status == 404) return null;
        if (status != 200) throw new IOException("GitHub menjawab kode " + status);
        String json;
        try (InputStream in = connection.getInputStream()) { json = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
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
                java.net.HttpURLConnection connection = openConnection(release.zipUrl());
                if (connection.getResponseCode() != 200) throw new IOException("Unduhan gagal, kode " + connection.getResponseCode());
                long total = connection.getContentLengthLong();
                try (InputStream in = connection.getInputStream(); OutputStream out = Files.newOutputStream(zip)) {
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
        label.setFont(font(true, 14));
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
        // Dialog pesan dan pemilih berkas memakai tampilan bawaan Windows; komponen utama digambar sendiri.
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        UIManager.put("ToolTip.background", PANEL);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_STRONG),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        UIManager.put("ToolTip.font", font(false, 12));
        // Hanya satu jendela Nomor Sorter per folder data: dua jendela bisa saling menimpa nomor.
        try {
            Files.createDirectories(Paths.get("data"));
            java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(Paths.get("data", ".kunci-aplikasi"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            appLock = channel.tryLock();
            if (appLock == null) {
                JOptionPane.showMessageDialog(null, "Nomor Sorter sudah terbuka di jendela lain.\nPakai jendela yang sudah terbuka agar nomor tidak saling menimpa.",
                    "Nomor Sorter", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            }
        } catch (IOException ex) { System.err.println("Kunci aplikasi tidak dapat dibuat: " + ex.getMessage()); }
        SwingUtilities.invokeLater(() -> {
            try { new NomorSorter().setVisible(true); }
            catch (IOException ex) { JOptionPane.showMessageDialog(null, "Gagal membuka basis data: " + ex.getMessage()); }
        });
    }
}
