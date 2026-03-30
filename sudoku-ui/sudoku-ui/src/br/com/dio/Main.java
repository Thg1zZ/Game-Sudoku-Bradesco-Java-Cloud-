package br.com.dio;

import br.com.dio.model.Board;
import br.com.dio.model.Space;
import br.com.dio.service.HintService;
import br.com.dio.service.TimerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import static br.com.dio.util.BoardTemplate.BOARD_TEMPLATE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;

public class Main {

    private final static Scanner scanner = new Scanner(System.in);

    private static Board board;
    private static TimerService timerService = new TimerService();
    private static HintService  hintService;

    private final static int BOARD_LIMIT = 9;

    public static void main(String[] args) {
        final var positions = Stream.of(args)
                .collect(toMap(
                        k -> k.split(";")[0],
                        v -> v.split(";")[1]
                ));
        while (true) {
            System.out.println("\n╔══════════════════════════════════╗");
            System.out.println("║         SUDOKU  -  MENU          ║");
            if (nonNull(board)) {
                System.out.printf( "║  ⏱  Tempo : %-20s║%n", timerService.getFormattedTime());
                System.out.printf( "║  💡 Dicas  : %d/%d restantes        ║%n",
                        hintService.getHintsRemaining(), HintService.MAX_HINTS);
            }
            System.out.println("╠══════════════════════════════════╣");
            System.out.println("║  1 - Iniciar um novo jogo        ║");
            System.out.println("║  2 - Colocar um número           ║");
            System.out.println("║  3 - Remover um número           ║");
            System.out.println("║  4 - Visualizar jogo atual       ║");
            System.out.println("║  5 - Verificar status do jogo    ║");
            System.out.println("║  6 - Usar uma dica 💡            ║");
            System.out.println("║  7 - Limpar jogo                 ║");
            System.out.println("║  8 - Finalizar jogo              ║");
            System.out.println("║  9 - Sair                        ║");
            System.out.println("╚══════════════════════════════════╝");
            System.out.print("Opção: ");

            var option = scanner.nextInt();

            switch (option) {
                case 1 -> startGame(positions);
                case 2 -> inputNumber();
                case 3 -> removeNumber();
                case 4 -> showCurrentGame();
                case 5 -> showGameStatus();
                case 6 -> useHint();
                case 7 -> clearGame();
                case 8 -> finishGame();
                case 9 -> {
                    timerService.stop();
                    System.out.println("Até logo!");
                    System.exit(0);
                }
                default -> System.out.println("Opção inválida, selecione uma das opções do menu");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Jogo
    // -------------------------------------------------------------------------

    private static void startGame(final Map<String, String> positions) {
        if (nonNull(board)) {
            System.out.println("O jogo já foi iniciado");
            return;
        }

        List<List<Space>> spaces = new ArrayList<>();
        for (int i = 0; i < BOARD_LIMIT; i++) {
            spaces.add(new ArrayList<>());
            for (int j = 0; j < BOARD_LIMIT; j++) {
                var positionConfig = positions.get("%s,%s".formatted(i, j));
                var expected = Integer.parseInt(positionConfig.split(",")[0]);
                var fixed    = Boolean.parseBoolean(positionConfig.split(",")[1]);
                spaces.get(i).add(new Space(expected, fixed));
            }
        }

        board       = new Board(spaces);
        hintService = new HintService(board, timerService);

        timerService.stop();   // garante estado limpo
        timerService.start();

        System.out.println("✅ O jogo está pronto para começar! O cronômetro foi iniciado.");
    }

    private static void inputNumber() {
        if (isNull(board)) { gameNotStarted(); return; }

        System.out.println("Informe a coluna (0-8):");
        var col = runUntilGetValidNumber(0, 8);
        System.out.println("Informe a linha (0-8):");
        var row = runUntilGetValidNumber(0, 8);
        System.out.printf("Informe o número para [%s,%s] (1-9):\n", col, row);
        var value = runUntilGetValidNumber(1, 9);

        if (!board.changeValue(col, row, value)) {
            System.out.printf("❌ A posição [%s,%s] tem um valor fixo\n", col, row);
        } else {
            System.out.printf("✅ Número %d inserido em [%s,%s]\n", value, col, row);
        }
    }

    private static void removeNumber() {
        if (isNull(board)) { gameNotStarted(); return; }

        System.out.println("Informe a coluna (0-8):");
        var col = runUntilGetValidNumber(0, 8);
        System.out.println("Informe a linha (0-8):");
        var row = runUntilGetValidNumber(0, 8);

        if (!board.clearValue(col, row)) {
            System.out.printf("❌ A posição [%s,%s] tem um valor fixo\n", col, row);
        } else {
            System.out.printf("✅ Valor removido de [%s,%s]\n", col, row);
        }
    }

    private static void showCurrentGame() {
        if (isNull(board)) { gameNotStarted(); return; }

        var args   = new Object[81];
        var argPos = 0;
        for (int i = 0; i < BOARD_LIMIT; i++) {
            for (var col : board.getSpaces()) {
                args[argPos++] = " " + (isNull(col.get(i).getActual()) ? " " : col.get(i).getActual());
            }
        }
        System.out.println("\nSeu jogo se encontra da seguinte forma:");
        System.out.printf((BOARD_TEMPLATE) + "\n", args);
        System.out.printf("⏱  Tempo decorrido : %s%n", timerService.getFormattedTime());
        System.out.printf("💡 Dicas restantes : %d/%d%n", hintService.getHintsRemaining(), HintService.MAX_HINTS);
    }

    private static void showGameStatus() {
        if (isNull(board)) { gameNotStarted(); return; }

        System.out.printf("📋 Status  : %s%n", board.getStatus().getLabel());
        System.out.printf("⏱  Tempo   : %s%n", timerService.getFormattedTime());
        System.out.printf("💡 Dicas   : %d/%d restantes (%d usadas)%n",
                hintService.getHintsRemaining(), HintService.MAX_HINTS, hintService.getHintsUsed());

        if (board.hasErrors()) {
            System.out.println("⚠️  O jogo contém erros");
        } else {
            System.out.println("✅ O jogo não contém erros");
        }
    }

    // -------------------------------------------------------------------------
    // Dicas
    // -------------------------------------------------------------------------

    private static void useHint() {
        if (isNull(board)) { gameNotStarted(); return; }

        if (hintService.getHintsRemaining() == 0) {
            System.out.println("❌ Você não possui mais dicas disponíveis.");
            return;
        }

        System.out.printf("💡 Você tem %d dica(s) restante(s). Cada dica adiciona %ds de penalidade. Deseja usar? (sim/não)%n",
                hintService.getHintsRemaining(), HintService.PENALTY_SECONDS);

        var confirm = scanner.next();
        if (!confirm.equalsIgnoreCase("sim")) {
            System.out.println("Dica cancelada.");
            return;
        }

        var result = hintService.requestHint();
        if (result.isPresent()) {
            var hint = result.get();
            System.out.printf("✅ Dica: a posição [col=%d, row=%d] deve ser %d%n",
                    hint.col(), hint.row(), hint.value());
            System.out.printf("⚠️  Penalidade de +%ds aplicada ao seu tempo.%n", HintService.PENALTY_SECONDS);
            System.out.printf("💡 Dicas restantes: %d/%d%n", hint.hintsRemaining(), HintService.MAX_HINTS);
        } else {
            System.out.println("Não há células vazias para revelar.");
        }
    }

    // -------------------------------------------------------------------------
    // Controles
    // -------------------------------------------------------------------------

    private static void clearGame() {
        if (isNull(board)) { gameNotStarted(); return; }

        System.out.println("Tem certeza que deseja limpar o jogo e perder todo o progresso? (sim/não)");
        var confirm = scanner.next();
        while (!confirm.equalsIgnoreCase("sim") && !confirm.equalsIgnoreCase("não")) {
            System.out.println("Informe 'sim' ou 'não'");
            confirm = scanner.next();
        }

        if (confirm.equalsIgnoreCase("sim")) {
            board.reset();
            timerService.stop();
            timerService.start();
            hintService.reset();
            System.out.println("🔄 Jogo limpo! Cronômetro e dicas reiniciados.");
        }
    }

    private static void finishGame() {
        if (isNull(board)) { gameNotStarted(); return; }

        if (board.gameIsFinished()) {
            timerService.pause();
            System.out.println("🎉 Parabéns! Você concluiu o jogo!");
            System.out.printf("⏱  Tempo final: %s (com %d dica(s) usada(s))%n",
                    timerService.getFormattedTime(), hintService.getHintsUsed());
            showCurrentGame();
            board       = null;
            hintService = null;
            timerService.stop();
        } else if (board.hasErrors()) {
            System.out.println("⚠️  Seu jogo contém erros, verifique o board e ajuste-o.");
        } else {
            System.out.println("📝 Você ainda precisa preencher algum espaço.");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void gameNotStarted() {
        System.out.println("⚠️  O jogo ainda não foi iniciado.");
    }

    private static int runUntilGetValidNumber(final int min, final int max) {
        var current = scanner.nextInt();
        while (current < min || current > max) {
            System.out.printf("Informe um número entre %s e %s:%n", min, max);
            current = scanner.nextInt();
        }
        return current;
    }
}
