# Requisitos não funcionais — baseline preservada no Acorde 2.0

> **Versão do sistema: 2.0.** Os requisitos não funcionais originais permanecem como compromissos da reformulação. O estado atual é documental; conformidade ainda não foi medida nem validada.

| Código | Tema | Exigência |
|---|---|---|
| RNF01 | Desempenho | Confirmação visual de criação, edição e consulta de registros e ações em até 5 segundos, sob condições de teste documentadas |
| RNF02 | Acessibilidade | Alinhamento às WCAG 2.1/2.2 (A/AA) e eMAG; teclado, leitores de tela, rótulos e contraste |
| RNF03 | Usabilidade | Interfaces compreensíveis, responsivas e alinhadas às boas práticas de UI/UX |
| RNF04 | Multiplataforma | Web e Mobile com objetivo de compatibilidade Android/iOS; registrar plataformas e dispositivos realmente validados |
| RNF05 | Green IT | Modo escuro e rotinas/consultas otimizadas para reduzir processamento e transferência de dados |
| RNF06 | Segurança e LGPD | Autenticação, autorização, isolamento de organizações, proteção de dados e minimização da coleta |

## Aplicação ao domínio de carbono

- Não coletar endereço residencial nem GPS automático para registrar deslocamento quando distância/modalidade informadas forem suficientes.
- Não expor dados pessoais ou registros individuais no dashboard organizacional além do necessário e autorizado.
- Restringir todo acesso por organização no backend; esconder controles na interface não substitui autorização no servidor.
- Proteger senhas com hash próprio para senhas, como BCrypt ou Argon2id, nunca criptografia reversível.
- Não registrar tokens, credenciais ou dados pessoais desnecessários em logs.
- Informar unidades, premissas e limitações dos indicadores. Evitar afirmações de impacto que dados não sustentem.

## Verificação

Definir cenários reproduzíveis para o limite de cinco segundos (ambiente, rede, volume, dispositivo e operação). Verificar navegação por teclado, textos e rótulos acessíveis, contraste, leitor de tela quando possível, autorização, isolamento entre organizações, minimização de dados e erros de rede. Compatibilidade não deve ser declarada sem teste registrado. Nenhuma dessas verificações está marcada como concluída.
