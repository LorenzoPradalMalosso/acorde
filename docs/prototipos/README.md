# Protótipos e fluxos — Acorde 2.0

> **Versão do sistema 2.0.** A plataforma dos protótipos ainda precisa de confirmação com a equipe. O histórico cita Figma e Google Stitch, e os links continuam pendentes de validação.

Antes de publicar URLs, confirmar acesso e permissões. Não incluir convites privados, credenciais ou dados pessoais. Usar identidade e tokens oficiais do Brand Kit.

O solicitante indicou o [Protótipo Acorde 2.0 em Verde Claro](Prot%C3%B3tipo%20Acorde%202.0%20em%20Verde%20Claro.png) como referência visual da implementação (D-013). A Web segue a composição do login com paisagem, menu lateral verde profundo, cartões do dashboard e metas com ações vinculadas. O Mobile segue a navegação de início, registros, ações e perfil, com telas dedicadas para registrar deslocamento e avançar o estado de uma ação. Os valores das telas vêm da API. O início Mobile destaca o último deslocamento com cálculo válido vigente. A aprovação acadêmica e a confirmação da ferramenta do protótipo seguem pendentes.

## Fluxos a representar no MVP

1. Login de gestor e colaborador, seleção/isolamento de organização quando aplicável.
2. Gestor registra consumo de energia com período e contexto regional necessário ao fator.
3. Colaborador registra deslocamento casa–trabalho com modalidade, distância de ida/volta, dias/viagens e ocupação quando pertinente; sem GPS automático.
4. Interface mostra o estado do cálculo, unidade, fonte/versão do fator e pendências de dados.
5. Dashboard apresenta emissões monitoradas por fonte e período, cobertura, histórico e comparação válida.
6. Gestor define meta com período-base e prazo, cria ação e atribui responsável.
7. Colaborador autorizado atualiza o estado da ação; painel separa progresso operacional de emissões medidas.

## Validação dos fluxos

| Fluxo | Link/captura autorizada | Requisito v2 | Validado com usuário? | Ajustes |
|---|---|---|---|---|
| Login/RBAC | A preencher | RF-v2-01/02 | Pendente | A preencher |
| Registro de energia Web | A preencher | RF-v2-03/04/05 | Pendente | A preencher |
| Registro de deslocamento Mobile | A preencher | RF-v2-03/04/05 | Pendente | A preencher |
| Histórico e dashboard | A preencher | RF-v2-06/09 | Pendente | A preencher |
| Metas e ações | A preencher | RF-v2-07/08 | Pendente | A preencher |

Prototipar estados vazios, pendentes por falta de fator/dados, erro de rede, aumento de emissões, cobertura incompleta e ausência de período-base. Não usar números fictícios sem indicação explícita.

## Paisagem do login Web

A imagem [floresta-acorde.jpg](../../frontend/public/branding/floresta-acorde.jpg) foi criada com o ImageGen integrado como ilustração decorativa. A direção visual seguiu o PNG local e a avaliação de hierarquia, legibilidade e responsividade da [Awwwards Skill](https://github.com/Muski360/awwwards-skill).

> Photorealistic vertical editorial landscape photograph for the left half of the Acorde carbon monitoring web login. Tall 2:3 composition inspired by green mountains in mist. Upper third airy pale sage morning sky and layers of distant rolling green mountain ridges, middle and foreground dense deep emerald Atlantic rainforest canopy with dark hills especially in central left and bottom, richly textured treetops at lower edge, gentle natural dawn haze. Calm restrained green and teal color grade. Keep center-left tonal values dark enough to overlay white logo and copy in the interface. Full bleed environment, natural photographic detail, no people, no buildings, no typography, no logo, no UI, no watermark.
