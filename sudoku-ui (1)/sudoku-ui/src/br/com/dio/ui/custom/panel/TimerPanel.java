package br.com.dio.ui.custom.panel;

import br.com.dio.service.TimerService;

import javax.swing.*;
import java.awt.*;

/**
 * Painel Swing que exibe o cronômetro em tempo real.
 * Atualiza a cada segundo via callback do {@link TimerService}.
 */
public class TimerPanel extends JPanel {

    private final TimerService timerService;
    private final JLabel timerLabel;
    private final JLabel titleLabel;

    public TimerPanel(final TimerService timerService) {
        this.timerService = timerService;

        setLayout(new FlowLayout(FlowLayout.CENTER, 8, 4));
        setOpaque(false);

        titleLabel = new JLabel("⏱ Tempo:");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(new Color(60, 60, 60));

        timerLabel = new JLabel("00:00");
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 22));
        timerLabel.setForeground(new Color(30, 100, 200));

        add(titleLabel);
        add(timerLabel);

        // Registra tick: atualiza o label na EDT a cada segundo
        timerService.startTicking(formatted ->
                SwingUtilities.invokeLater(() -> timerLabel.setText(formatted))
        );
    }

    /** Para os ticks periódicos delegando para o {@link TimerService}. */
    public void stopTick() {
        timerService.stopTick();
    }

    /** Reinicia a exibição para 00:00. */
    public void reset() {
        SwingUtilities.invokeLater(() -> timerLabel.setText("00:00"));
    }

    /** Muda a cor do timer (ex: vermelho ao terminar). */
    public void setTimerColor(Color color) {
        SwingUtilities.invokeLater(() -> timerLabel.setForeground(color));
    }
}