package br.com.dio.ui.custom.panel;

import br.com.dio.service.HintService;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Painel Swing que exibe o botão de dica e o contador de dicas restantes.
 *
 * <p>Ao clicar no botão, um diálogo de confirmação é exibido informando
 * a penalidade de tempo antes de revelar a dica.</p>
 */
public class HintPanel extends JPanel {

    private final HintService hintService;
    private final JLabel hintCountLabel;
    private final JButton hintButton;

    /** Callback disparado após uma dica ser usada (recebe mensagem de resultado). */
    private Consumer<String> onHintUsed = msg -> {};

    public HintPanel(final HintService hintService) {
        this.hintService = hintService;

        setLayout(new FlowLayout(FlowLayout.CENTER, 8, 4));
        setOpaque(false);

        hintCountLabel = new JLabel(buildHintText());
        hintCountLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        hintCountLabel.setForeground(new Color(60, 60, 60));

        hintButton = new JButton("💡 Dica");
        hintButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        hintButton.setBackground(new Color(255, 210, 50));
        hintButton.setFocusPainted(false);
        hintButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hintButton.addActionListener(e -> handleHintRequest());

        add(hintCountLabel);
        add(hintButton);
    }

    public void setOnHintUsed(Consumer<String> callback) {
        this.onHintUsed = callback;
    }

    /** Atualiza o painel após um reset de jogo. */
    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            hintCountLabel.setText(buildHintText());
            hintButton.setEnabled(hintService.getHintsRemaining() > 0);
        });
    }

    // -------------------------------------------------------------------------

    private void handleHintRequest() {
        if (hintService.getHintsRemaining() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Você não possui mais dicas disponíveis.",
                    "Sem dicas", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><b>Usar uma dica?</b><br>" +
                "Dicas restantes: <b>" + hintService.getHintsRemaining() + "/" + HintService.MAX_HINTS + "</b><br>" +
                "⚠️ Penalidade: <b>+" + HintService.PENALTY_SECONDS + "s</b> no cronômetro.</html>",
                "Confirmar dica",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        hintService.requestHint().ifPresentOrElse(hint -> {
            String msg = "<html>✅ Dica revelada!<br>" +
                         "Posição <b>[col=%d, row=%d]</b> = <b>%d</b><br>"
                             .formatted(hint.col(), hint.row(), hint.value()) +
                         "⚠️ Penalidade de +%ds aplicada.<br>".formatted(HintService.PENALTY_SECONDS) +
                         "💡 Dicas restantes: <b>%d/%d</b></html>"
                             .formatted(hint.hintsRemaining(), HintService.MAX_HINTS);

            JOptionPane.showMessageDialog(this, msg, "Dica usada!", JOptionPane.INFORMATION_MESSAGE);
            refresh();
            onHintUsed.accept(msg);
        }, () -> JOptionPane.showMessageDialog(this,
                "Não há células vazias para revelar.",
                "Nenhuma dica disponível", JOptionPane.INFORMATION_MESSAGE));
    }

    private String buildHintText() {
        return "💡 Dicas: %d/%d".formatted(hintService.getHintsRemaining(), HintService.MAX_HINTS);
    }
}
