package br.com.dio.ui.custom.screen;

import br.com.dio.service.BoardService;
import br.com.dio.service.HintService;
import br.com.dio.service.TimerService;
import br.com.dio.ui.custom.panel.HintPanel;
import br.com.dio.ui.custom.panel.TimerPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static br.com.dio.model.GameStatusEnum.NON_STARTED;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Tela principal do Sudoku com suporte a cronômetro e dicas.
 *
 * <p>Layout:</p>
 * <pre>
 *  ┌─────────────────────────────────────┐
 *  │          Título                     │
 *  │  ⏱ 00:00          💡 Dicas: 3/3    │  ← headerBar
 *  ├─────────────────────────────────────┤
 *  │                                     │
 *  │        Grid 9×9 do Sudoku           │  ← boardPanel
 *  │                                     │
 *  ├─────────────────────────────────────┤
 *  │  [Nova Partida] [Verificar] [Sair]  │  ← buttonBar
 *  └─────────────────────────────────────┘
 * </pre>
 */
public class MainScreen {

    // ── Serviços ────────────────────────────────────────────────────────────
    private final Map<String, String> gameConfig;
    private BoardService boardService;
    private TimerService timerService;
    private HintService  hintService;

    // ── UI ──────────────────────────────────────────────────────────────────
    private JFrame      frame;
    private JPanel      boardPanel;
    private TimerPanel  timerPanel;
    private HintPanel   hintPanel;

    /** Cells indexed [col][row] */
    private JTextField[][] cells;

    private static final int BOARD_SIZE  = 9;
    private static final int CELL_SIZE   = 56;
    private static final Color COLOR_FIXED    = new Color(230, 235, 255);
    private static final Color COLOR_EDITABLE = Color.WHITE;
    private static final Color COLOR_ERROR    = new Color(255, 210, 210);
    private static final Color COLOR_HINT     = new Color(210, 255, 220);

    public MainScreen(final Map<String, String> gameConfig) {
        this.gameConfig = gameConfig;
    }

    // ── Entry point ─────────────────────────────────────────────────────────

    public void buildMainScreen() {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Sudoku");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setResizable(false);

            var content = new JPanel(new BorderLayout(0, 0));
            content.setBorder(new EmptyBorder(12, 12, 12, 12));
            content.setBackground(new Color(245, 245, 250));

            content.add(buildTitlePanel(),  BorderLayout.NORTH);
            content.add(buildHeaderBar(),   BorderLayout.CENTER); // será um wrapper
            content.add(buildButtonBar(),   BorderLayout.SOUTH);

            // Monta manualmente para ter título + header + board empilhados
            var center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);
            center.add(buildHeaderBar());
            center.add(Box.createVerticalStrut(8));
            center.add(buildBoardPanel());

            content.add(center, BorderLayout.CENTER);

            frame.setContentPane(content);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Inicia jogo automaticamente
            initGame();
        });
    }

    // ── Construção dos painéis ───────────────────────────────────────────────

    private JPanel buildTitlePanel() {
        var label = new JLabel("SUDOKU", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 28));
        label.setForeground(new Color(30, 80, 180));
        label.setBorder(new EmptyBorder(0, 0, 8, 0));

        var p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(label, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildHeaderBar() {
        // Serviços ainda não inicializados; os painéis serão criados em initGame()
        // Criamos um container que será preenchido depois.
        var bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 220)),
                new EmptyBorder(4, 0, 8, 0)
        ));

        // Placeholders — serão substituídos em initGame()
        timerPanel = new TimerPanel(new TimerService()); // dummy inicial
        hintPanel  = new HintPanel(createDummyHintService());

        bar.add(timerPanel, BorderLayout.WEST);
        bar.add(hintPanel,  BorderLayout.EAST);
        return bar;
    }

    private JPanel buildBoardPanel() {
        boardPanel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE, 1, 1));
        boardPanel.setBackground(new Color(80, 80, 120));
        boardPanel.setBorder(BorderFactory.createLineBorder(new Color(30, 30, 80), 3));

        cells = new JTextField[BOARD_SIZE][BOARD_SIZE];
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                cells[col][row] = createCell(col, row);
                boardPanel.add(cells[col][row]);
            }
        }
        return boardPanel;
    }

    private JPanel buildButtonBar() {
        var bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        bar.setOpaque(false);

        bar.add(createButton("🔄 Nova Partida", new Color(70, 130, 200),  e -> restartGame()));
        bar.add(createButton("✅ Verificar",     new Color(60, 170, 90),   e -> checkGame()));
        bar.add(createButton("🏁 Finalizar",     new Color(200, 100, 50),  e -> finishGame()));
        bar.add(createButton("❌ Sair",           new Color(170, 60, 60),   e -> System.exit(0)));
        return bar;
    }

    // ── Células do tabuleiro ─────────────────────────────────────────────────

    private JTextField createCell(int col, int row) {
        var cell = new JTextField();
        cell.setHorizontalAlignment(SwingConstants.CENTER);
        cell.setFont(new Font("SansSerif", Font.BOLD, 20));
        cell.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
        cell.setBorder(buildCellBorder(col, row));

        // Listener: atualiza board ao editar
        cell.addActionListener(e -> onCellEdited(cell, col, row));
        cell.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                onCellEdited(cell, col, row);
            }
        });
        return cell;
    }

    private void onCellEdited(JTextField cell, int col, int row) {
        if (isNull(boardService)) return;
        String text = cell.getText().trim();
        if (text.isEmpty()) {
            boardService.clearValue(col, row);
            cell.setBackground(COLOR_EDITABLE);
            return;
        }
        try {
            int value = Integer.parseInt(text);
            if (value < 1 || value > 9) throw new NumberFormatException();
            if (!boardService.changeValue(col, row, value)) {
                cell.setBackground(COLOR_FIXED);
            } else {
                // Validação imediata de erros
                var space = boardService.getSpaces().get(col).get(row);
                boolean wrong = nonNull(space.getActual()) && !space.getActual().equals(space.getExpected());
                cell.setBackground(wrong ? COLOR_ERROR : COLOR_EDITABLE);
            }
        } catch (NumberFormatException ex) {
            cell.setText("");
            boardService.clearValue(col, row);
        }
    }

    // ── Lógica de jogo ───────────────────────────────────────────────────────

    private void initGame() {
        boardService = new BoardService(gameConfig);
        timerService = new TimerService();
        hintService  = new HintService(boardService.getBoard(), timerService);

        // Substitui os componentes de header pelos reais
        timerPanel.stopTick();
        timerPanel = new TimerPanel(timerService);
        hintPanel  = new HintPanel(hintService);
        hintPanel.setOnHintUsed(msg -> refreshBoardFromService());

        // Atualiza o header bar (o segundo filho do center, que é o headerBar)
        rebuildHeaderBar();

        refreshBoardFromService();

        timerService.start();
        timerPanel = new TimerPanel(timerService); // reconecta ticking
        rebuildHeaderBar();
    }

    private void refreshBoardFromService() {
        if (isNull(boardService)) return;
        var spaces = boardService.getSpaces();
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                var space = spaces.get(col).get(row);
                var cell  = cells[col][row];
                if (space.isFixed()) {
                    cell.setText(String.valueOf(space.getActual()));
                    cell.setEditable(false);
                    cell.setBackground(COLOR_FIXED);
                    cell.setForeground(new Color(30, 30, 150));
                } else if (nonNull(space.getActual())) {
                    cell.setText(String.valueOf(space.getActual()));
                    cell.setEditable(true);
                    // Destaque especial para células reveladas por dica
                    cell.setBackground(COLOR_HINT);
                    cell.setForeground(new Color(20, 140, 60));
                } else {
                    cell.setText("");
                    cell.setEditable(true);
                    cell.setBackground(COLOR_EDITABLE);
                    cell.setForeground(Color.BLACK);
                }
            }
        }
        hintPanel.refresh();
        frame.revalidate();
        frame.repaint();
    }

    private void restartGame() {
        int confirm = JOptionPane.showConfirmDialog(frame,
                "Deseja reiniciar o jogo? Todo o progresso será perdido.",
                "Nova partida", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        timerService.stop();
        timerPanel.stopTick();
        timerPanel.reset();

        boardService.reset();
        hintService.reset();

        timerService = new TimerService();
        hintService  = new HintService(boardService.getBoard(), timerService);
        hintPanel    = new HintPanel(hintService);
        hintPanel.setOnHintUsed(msg -> refreshBoardFromService());
        timerPanel   = new TimerPanel(timerService);
        timerService.start();
        rebuildHeaderBar();
        refreshBoardFromService();
    }

    private void checkGame() {
        if (isNull(boardService)) return;
        if (boardService.hasErrors()) {
            JOptionPane.showMessageDialog(frame,
                    "<html>⚠️ O jogo contém <b>erros</b>.<br>Verifique as células em vermelho.</html>",
                    "Verificação", JOptionPane.WARNING_MESSAGE);
        } else if (boardService.getStatus() == NON_STARTED) {
            JOptionPane.showMessageDialog(frame, "Você ainda não preencheu nenhuma célula.",
                    "Verificação", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame,
                    "<html>✅ Nenhum erro encontrado até agora!<br>Tempo: <b>"
                    + timerService.getFormattedTime() + "</b></html>",
                    "Verificação", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void finishGame() {
        if (isNull(boardService)) return;
        if (boardService.gameIsFinished()) {
            timerService.pause();
            String time   = timerService.getFormattedTime();
            int hintsUsed = hintService.getHintsUsed();
            timerPanel.setTimerColor(new Color(20, 160, 70));
            JOptionPane.showMessageDialog(frame,
                    "<html>🎉 <b>Parabéns! Você concluiu o Sudoku!</b><br><br>" +
                    "⏱ Tempo final : <b>" + time + "</b><br>" +
                    "💡 Dicas usadas: <b>" + hintsUsed + "/" + HintService.MAX_HINTS + "</b></html>",
                    "Jogo concluído! 🏆", JOptionPane.INFORMATION_MESSAGE);
        } else if (boardService.hasErrors()) {
            JOptionPane.showMessageDialog(frame,
                    "⚠️ Seu jogo contém erros. Corrija antes de finalizar.",
                    "Erros encontrados", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame,
                    "📝 Ainda há células vazias para preencher.",
                    "Incompleto", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void rebuildHeaderBar() {
        // Localiza o header bar no layout e substitui os componentes
        SwingUtilities.invokeLater(() -> {
            // O header bar é o primeiro filho do painel "center"
            // Navegamos pela hierarquia: content > center > headerBar (index 0)
            var content = (JPanel) frame.getContentPane();
            var center  = (JPanel) content.getComponent(1); // CENTER
            var header  = (JPanel) center.getComponent(0);

            header.removeAll();
            header.add(timerPanel, BorderLayout.WEST);
            header.add(hintPanel,  BorderLayout.EAST);
            header.revalidate();
            header.repaint();
        });
    }

    private JButton createButton(String text, Color bg, java.awt.event.ActionListener listener) {
        var btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                new EmptyBorder(6, 14, 6, 14)
        ));
        btn.addActionListener(listener);
        return btn;
    }

    /**
     * Borda CSS-like: bordas mais grossas entre os quadrantes 3×3.
     */
    private javax.swing.border.Border buildCellBorder(int col, int row) {
        int top    = (row % 3 == 0) ? 3 : 1;
        int left   = (col % 3 == 0) ? 3 : 1;
        int bottom = (row == 8)      ? 3 : 1;
        int right  = (col == 8)      ? 3 : 1;
        return BorderFactory.createMatteBorder(top, left, bottom, right,
                new Color(80, 80, 120));
    }

    private HintService createDummyHintService() {
        // Serviço vazio apenas para não lançar NPE antes do initGame()
        return new HintService(null, new TimerService()) {
            @Override public java.util.Optional<br.com.dio.service.HintService.HintResult> requestHint() {
                return java.util.Optional.empty();
            }
            @Override public int getHintsRemaining() { return HintService.MAX_HINTS; }
        };
    }
}
