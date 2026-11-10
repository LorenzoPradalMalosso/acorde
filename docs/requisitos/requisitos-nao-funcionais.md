# Requisitos não funcionais — baseline original

| Código | Tema | Exigência |
| --- | --- | --- |
| RNF01 | Desempenho | Criação, edição e consulta de tarefas e eventos com confirmação visual em até 5 segundos |
| RNF02 | Acessibilidade | WCAG 2.1/2.2 (A/AA) e eMAG, leitores de tela, teclado e contraste |
| RNF03 | Usabilidade | Interfaces intuitivas, responsivas e alinhadas às boas práticas de UI/UX |
| RNF04 | Multiplataforma | Aplicação Web e aplicativo Mobile compatível com Android e iOS |
| RNF05 | Green IT | Dark Mode, rotinas e consultas otimizadas para reduzir processamento e tráfego |
| RNF06 | Segurança/LGPD | Proteção de dados, autenticação e controle de acesso |

## Orientações de implementação

- Proteger senhas com hash apropriado para senhas (não criptografia reversível).
- Definir cenários de teste e formas reproduzíveis de medir o RNF01.
- Incluir navegação por teclado, textos alternativos, rótulos e estados de erro acessíveis.
- Verificar permissões no backend e não registrar informações sensíveis em logs.
- Distinguir compatibilidade de plataforma de validação em dispositivo real.
