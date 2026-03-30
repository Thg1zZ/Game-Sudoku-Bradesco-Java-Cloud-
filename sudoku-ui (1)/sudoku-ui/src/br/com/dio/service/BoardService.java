package br.com.dio.service;

import br.com.dio.model.Board;
import br.com.dio.model.GameStatusEnum;
import br.com.dio.model.Space;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoardService {

    private final static int BOARD_LIMIT = 9;

    private final Board board;

    public BoardService(final Map<String, String> gameConfig) {
        this.board = new Board(initBoard(gameConfig));
    }

    // ── Acesso ao modelo ─────────────────────────────────────────────────────

    /** Retorna o {@link Board} subjacente (necessário para o {@link HintService}). */
    public Board getBoard() {
        return board;
    }

    public List<List<Space>> getSpaces() {
        return board.getSpaces();
    }

    // ── Operações ────────────────────────────────────────────────────────────

    public void reset() {
        board.reset();
    }

    /**
     * Altera o valor de uma célula.
     *
     * @return {@code false} se a célula for fixa
     */
    public boolean changeValue(final int col, final int row, final int value) {
        return board.changeValue(col, row, value);
    }

    /**
     * Limpa o valor de uma célula.
     *
     * @return {@code false} se a célula for fixa
     */
    public boolean clearValue(final int col, final int row) {
        return board.clearValue(col, row);
    }

    // ── Status ───────────────────────────────────────────────────────────────

    public boolean hasErrors() {
        return board.hasErrors();
    }

    public GameStatusEnum getStatus() {
        return board.getStatus();
    }

    public boolean gameIsFinished() {
        return board.gameIsFinished();
    }

    // ── Inicialização ────────────────────────────────────────────────────────

    private List<List<Space>> initBoard(final Map<String, String> gameConfig) {
        List<List<Space>> spaces = new ArrayList<>();
        for (int i = 0; i < BOARD_LIMIT; i++) {
            spaces.add(new ArrayList<>());
            for (int j = 0; j < BOARD_LIMIT; j++) {
                var positionConfig = gameConfig.get("%s,%s".formatted(i, j));
                var expected = Integer.parseInt(positionConfig.split(",")[0]);
                var fixed    = Boolean.parseBoolean(positionConfig.split(",")[1]);
                spaces.get(i).add(new Space(expected, fixed));
            }
        }
        return spaces;
    }
}
