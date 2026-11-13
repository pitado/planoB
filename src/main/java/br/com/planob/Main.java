package br.com.planob;

import br.com.planob.domain.Localizacao;
import br.com.planob.domain.Ocorrencia;
import br.com.planob.domain.OcorrenciaFactory;
import br.com.planob.domain.TipoOcorrencia;

/** Ponto de entrada temporário enquanto interface e persistência são implementadas nas sprints. */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        Ocorrencia demonstracao = OcorrenciaFactory.nova(
                TipoOcorrencia.VIA_OBSTRUIDA,
                new Localizacao("Bairro fictício", "Rua Exemplo"),
                "Ocorrência simulada para verificar o domínio.",
                3);

        System.out.println("Plano B — núcleo de domínio em desenvolvimento");
        System.out.println("Exemplo simulado: " + demonstracao.tipo().descricao());
        System.out.println("Índice orientativo: " + demonstracao.calcularIndiceImpacto());
        System.out.println("Módulos de interface e PostgreSQL: sprints de Lucas e Gustavo.");
    }
}
