package fintrack.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import fintrack.db.Conexao;
import fintrack.exception.ValorInvalidoException;
import fintrack.model.Transacao;
import fintrack.model.TransacaoMensal;


/**
 * Classe que contém todos os metodos de interface com o banco
 * TransacaoDao
 */
public class TransacaoDao {
    public TransacaoDao() {
        criarTabela();
    }

    /**
     * Responsável por criar a tabela no SQLite
     */
    private void criarTabela() {
        String sql = """
            CREATE TABLE IF NOT EXISTS transacoes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                descricao VARCHAR(100),
                valor INTEGER,
                tipo VARCHAR(20),
                data DATE
            );
            """;
        try (Connection conexao = Conexao.conectar();
             Statement stmt = conexao.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Erro ao criar tabela: " + e.getMessage());
        }
    }

    /**
     * Garante que a transação será adicionada ao banco
     * @param t uma transação qualquer
     * @throws SQLException caso ocorra erro ao salvar
     */
    public void salvar(Transacao t) throws SQLException {
        String sql = "INSERT INTO transacoes (descricao, valor, tipo, data) VALUES(?, ?, ?, ?)";
        try (Connection conexao = Conexao.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, t.getDescricao());
            stmt.setLong(2, Math.round(t.getValor() * 100));
            stmt.setString(3, t.getTipo());
            stmt.setDate(4, Date.valueOf(t.getData()));
            stmt.executeUpdate();
        }
    }

    /**
     * Método responsável por trazer uma lista contendo
     * todas as transações salvas no banco
     * @return uma lista de transações
     * @throws SQLException caso ocorra erro ao consultar o banco
     */
    public List<Transacao> buscarTodos() throws SQLException {
        List<Transacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM transacoes";
        try (Connection conexao = Conexao.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String tipo = rs.getString("tipo");
                int id = rs.getInt("id");
                String descricao = rs.getString("descricao");
                double valor = rs.getLong("valor") / 100.0;
                var data = rs.getDate("data").toLocalDate();

                if ("Receita mensal".equals(tipo) || "Despesa mensal".equals(tipo)) {
                    lista.add(new TransacaoMensal(id, descricao, valor, tipo, data));
                } else {
                    lista.add(new Transacao(id, descricao, valor, tipo, data));
                }
            }
        } catch (ValorInvalidoException e) {
            throw new SQLException("Registro inválido no banco: " + e.getMessage(), e);
        }
        return lista;
    }

    /**
     * Remove a transação a partir do
     * id informado
     * @param id id da transação a ser removida
     * @throws SQLException caso ocorra erro ao remover
     */
    public void remover(int id) throws SQLException {
        String sql = "DELETE FROM transacoes WHERE id = ?";
        try (Connection conexao = Conexao.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Devolve a soma de todas as transações
     * contidas no banco
     * @return saldo total
     * @throws SQLException caso ocorra erro ao consultar o banco
     */
    public double buscarSaldo() throws SQLException {
        String sql = """
            SELECT SUM (
                CASE
                    WHEN tipo IN ('Receita', 'Receita mensal') THEN valor
                    WHEN tipo IN ('Despesa', 'Despesa mensal') THEN -valor
                    ELSE 0
                END
            ) FROM transacoes
            """;
        try (Connection conexao = Conexao.conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            long saldo = rs.next() ? rs.getLong(1) : 0L;
            return saldo / 100.0;
        }
    }
}