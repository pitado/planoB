#!/usr/bin/env python3
"""Gera documento formal e claramente retrospectivo para discussão com o professor."""
from pathlib import Path
from reportlab.lib.pagesizes import A4
from reportlab.lib.colors import HexColor, white
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_LEFT, TA_CENTER
from reportlab.lib import colors
from reportlab.platypus import (SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
                                PageBreak, Image, KeepTogether)
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.units import mm

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'docs' / 'Plano-B-AEP-Documentacao.pdf'
OUT.parent.mkdir(parents=True, exist_ok=True)

FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
FONT_BOLD = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
pdfmetrics.registerFont(TTFont('DejaVu', FONT))
pdfmetrics.registerFont(TTFont('DejaVu-Bold', FONT_BOLD))
pdfmetrics.registerFontFamily('DejaVu', normal='DejaVu', bold='DejaVu-Bold')

OLIVE = HexColor('#304B38')
MUTED = HexColor('#65776C')
BEIGE = HexColor('#F4F3EC')
BORDER = HexColor('#DDE2D7')
YELLOW = HexColor('#DFBD86')

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name='TitleHeroB', fontName='DejaVu-Bold', fontSize=32, leading=39, textColor=OLIVE, spaceAfter=18))
styles.add(ParagraphStyle(name='SubtitleB', fontName='DejaVu', fontSize=14, leading=22, textColor=MUTED, spaceAfter=15))
styles.add(ParagraphStyle(name='H1B', fontName='DejaVu-Bold', fontSize=16, leading=21, textColor=OLIVE, spaceAfter=11, spaceBefore=7))
styles.add(ParagraphStyle(name='H2B', fontName='DejaVu-Bold', fontSize=11.2, leading=15, textColor=OLIVE, spaceAfter=7, spaceBefore=10))
styles.add(ParagraphStyle(name='TextB', fontName='DejaVu', fontSize=9.35, leading=15.3, textColor=HexColor('#304139'), spaceAfter=9))
styles.add(ParagraphStyle(name='SmallB', fontName='DejaVu', fontSize=8, leading=12.8, textColor=MUTED, spaceAfter=7))
styles.add(ParagraphStyle(name='TinyB', fontName='DejaVu', fontSize=7.3, leading=11, textColor=HexColor('#304139')))
styles.add(ParagraphStyle(name='TinyBoldB', fontName='DejaVu-Bold', fontSize=7.2, leading=11, textColor=OLIVE))

story=[]
def P(txt, kind='TextB'): story.append(Paragraph(txt, styles[kind]))
def H(txt): P(txt, 'H1B')
def H2(txt): P(txt, 'H2B')
def S(h): story.append(Spacer(1,h))
def new(): story.append(PageBreak())
def tbl(head, rows, widths, tiny=True):
    headrow=[Paragraph(str(c),styles['TinyBoldB']) for c in head]
    body=[[Paragraph(str(c),styles['TinyB']) for c in row] for row in rows]
    t=Table([headrow]+body,colWidths=widths,repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([
        ('BACKGROUND',(0,0),(-1,0),HexColor('#E5ECDD')),
        ('ROWBACKGROUNDS',(0,1),(-1,-1),[white, HexColor('#FAFBF8')]),
        ('GRID',(0,0),(-1,-1),.35,BORDER),
        ('VALIGN',(0,0),(-1,-1),'TOP'),
        ('LEFTPADDING',(0,0),(-1,-1),9),('RIGHTPADDING',(0,0),(-1,-1),9),
        ('TOPPADDING',(0,0),(-1,-1),9),('BOTTOMPADDING',(0,0),(-1,-1),9)
    ]))
    story.append(t)

def picture(path,max_w,max_h):
    from PIL import Image as PILImage
    with PILImage.open(path) as i:w,h=i.size
    scale=min(max_w/w,max_h/h)
    story.append(Image(str(path),width=w*scale,height=h*scale,hAlign='CENTER'))

def footer(canv,doc):
    canv.saveState()
    canv.setStrokeColor(BORDER)
    canv.line(20*mm,17*mm,190*mm,17*mm)
    canv.setFont('DejaVu',7)
    canv.setFillColor(MUTED)
    canv.drawString(20*mm,12*mm,'PLANO B  •  AEP 2026.2  •  documento elaborado no 2º bimestre')
    canv.drawRightString(190*mm,12*mm,f'{doc.page}')
    canv.restoreState()

S(90)
P('PLANO B', 'TitleHeroB')
P('Plataforma de resiliência urbana e organização da recuperação após eventos climáticos', 'SubtitleB')
S(28)
P('<b>Atividade de Estudo Programada (AEP)</b><br/>ADS / Engenharia de Software — 4º semestre — 2026.2')
P('<b>ODS:</b> 11 (Cidades e comunidades sustentáveis) e 13 (Ação climática).')
S(30)
P('<b>Natureza do documento:</b> concepção, arquitetura e documentação de apoio à implementação do 2º bimestre.', 'H2B')
P('<b>Transparência acadêmica:</b> a entrega do 1º bimestre não foi realizada. Este documento foi elaborado posteriormente e não deve ser apresentado como comprovação de entrega anterior. A pontuação de aderência deve ser discutida com o professor.', 'TextB')
S(45)
P('Maringá, Paraná  •  13 de novembro de 2026', 'SmallB')
P('Integrantes e responsabilidades: a preencher de acordo com a participação real.', 'SmallB')
new()

H('1. Descoberta, problema e público')
P('Após interrupções urbanas severas, moradores e estabelecimentos frequentemente precisam registrar o que ocorreu, identificar possíveis impactos sobre a rotina e organizar passos de recuperação. Sem um histórico estruturado, a situação de cada ponto e as tarefas pendentes ficam dispersas.')
H2('Público interessado')
P('Moradores, organizações de bairro, gestores de pequenos comércios e equipes de estudos sobre resiliência. A ferramenta desenvolvida é um protótipo acadêmico, não um sistema oficial de Defesa Civil.')
H2('Origem e contexto')
P('O tema é inspirado na experiência local de temporais e danos relatados em Maringá (PR) em novembro de 2026. Todos os registros iniciais da aplicação são cenários <b>fictícios</b>; não são um levantamento oficial das ocorrências.')
H2('Proposta e alinhamento aos ODS')
P('<b>ODS 11:</b> organização comunitária de informações sobre interrupções e recuperação. <b>ODS 13:</b> estímulo ao planejamento da continuidade perante eventos meteorológicos extremos. O aplicativo não prediz desastres nem emite alertas oficiais.')
H2('Principais regras de negócio')
P('Cada ocorrência possui um tipo, localização, descrição, severidade entre 1 e 5, data e situação. Um índice orientativo de prioridade é calculado por subtipo de ocorrência com um peso diferenciado. Cada ocorrência pode conter diversas ações de recuperação e um histórico de suas mudanças de situação.')
H2('Fluxo de valor')
P('Registrar ocorrência → consultar situação e índice orientativo → definir ações e responsáveis → acompanhar progresso → atualizar situação → consultar histórico. A decisão operacional em uma emergência deve sempre recorrer aos canais oficiais.')
new()

H('2. Requisitos funcionais')
rows=[
('RF01','Cadastrar ocorrências com tipo, localização, descrição e severidade.'),
('RF02','Consultar e listar ocorrências, incluindo filtros por tipo e situação.'),
('RF03','Atualizar os dados e a situação de uma ocorrência existente.'),
('RF04','Excluir uma ocorrência cadastrada e seus registros dependentes.'),
('RF05','Classificar um índice de prioridade orientativo por tipo e severidade.'),
('RF06','Adicionar e concluir ações de recuperação vinculadas à ocorrência.'),
('RF07','Registrar automaticamente o histórico de alterações de situação.'),
('RF08','Mostrar um painel com totais de ocorrências por situação.'),
]
tbl(['Código','O sistema deve...'],rows,[48,430])
S(15)
H2('Requisitos não funcionais')
P('RNF01: compatibilidade com Java 21 e PostgreSQL 16; RNF02: uso de JDBC e parâmetros SQL; RNF03: interface web local e responsiva; RNF04: execução reproduzível por Docker Compose; RNF05: testes de domínio e integração isolada.')
H2('Limites do escopo')
P('Não estão incluídos despacho de equipes, reconhecimento de risco real, autenticação, geolocalização automática, atualização meteorológica, integração governamental ou publicação pública. Esses itens exigem controles e validações adicionais.')
new()

H('3. Justificativa técnica e arquitetura')
tbl(['Escolha','Razão técnica'],[
('Java 21','Tipagem, abstração, herança, polimorfismo e sobrescrita com @Override exigidos pela rubrica.'),
('PostgreSQL 16','Armazenamento relacional persistente, chaves estrangeiras, transações e integridade referencial.'),
('JDBC','Permite evidenciar SQL parametrizado e a conexão real com o banco sem esconder o CRUD em um ORM.'),
('Servidor HTTP do JDK','Interface local com poucas dependências e testes HTTP executáveis no JDK.'),
('Camadas','Separação entre domínio, repositórios, casos de uso, páginas e entrada HTTP.'),
('Docker Compose','Banco de demonstração reproduzível e scripts SQL versionados na pasta /database.'),
],[113,365])
S(20)
H2('Arquitetura lógica')
P('<b>Navegador</b> → servidor HTTP (`WebApp`) → serviço (`OcorrenciaService`) → contratos (`OcorrenciaRepository` / `AcaoRepository`) → implementação JDBC → <b>PostgreSQL</b>. As classes do domínio concentram validações e o cálculo polimórfico da prioridade.')
H2('Pilares de POO demonstrados')
P('<b>Classe abstrata:</b> `Ocorrencia`; <b>Herança:</b> `FalhaEnergia`, `ViaObstruida`, `Alagamento` e `DanoEstrutural`; <b>Polimorfismo:</b> cada classe sobrescreve `pesoOperacional()` com `@Override`; <b>Interfaces:</b> contratos de persistência; <b>Composição 1:N:</b> uma ocorrência possui várias ações de recuperação.')
H2('Persistência e integridade')
P('O script `database/schema.sql` define tabelas de ocorrências, ações e histórico, com chaves primárias e estrangeiras. Criar e atualizar uma ocorrência usa transação para registrar sua situação junto do histórico. Excluir ocorrência remove ações e histórico vinculados via ON DELETE CASCADE.')
new()

H('4. Diagrama de classes UML')
P('Relacionamentos de herança, implementação de interface e composição. O método `pesoOperacional()` sobrescrito em cada subclasse demonstra polimorfismo real.', 'SmallB')
picture(ROOT/'docs/diagrams/classes.png', 475, 655)
new()

H('5. DER — Diagrama Entidade-Relacionamento')
P('Modelo relacional do sistema, incluindo associações 1:N de ocorrências com ações e com alterações de situação.', 'SmallB')
S(10)
picture(ROOT/'docs/diagrams/der.png', 475, 420)
S(18)
P('A entidade principal do CRUD é `ocorrencias`. As entidades dependentes são vinculadas por `ocorrencia_id`; a integridade é implementada pelo PostgreSQL, não por listas em memória.', 'TextB')
new()

H('6. Cronograma e responsabilidades')
P('Este cronograma foi elaborado para documentar a execução no segundo bimestre. Não corresponde a um cronograma apresentado anteriormente.', 'SmallB')
tbl(['Data referência','Atividade','Responsável'],[
('13/11/2026','Escopo, levantamento de dores e requisitos','Equipe (a definir)'),
('13/11/2026','Domínio, POO, UML e DER','Equipe (a definir)'),
('13/11/2026','Banco de dados, CRUD e histórico','Equipe (a definir)'),
('13/11/2026','Interface, fluxos e integração','Equipe (a definir)'),
('13/11/2026','Testes, documentação e revisão','Equipe (a definir)'),
],[95,250,133])
S(23)
H2('Repositório GitHub')
P('Repositório público: <link href="https://github.com/pitado/planoB" color="blue">https://github.com/pitado/planoB</link>. O código foi organizado em etapas de desenvolvimento no histórico Git, sem simular participação de colegas. A primeira entrega não foi realizada.')
H2('Estrutura de repositório')
P('`src/main/java` (domínio, serviço, JDBC e web), `src/test/java` (testes), `database` (schema e seed), `docs` (arquitetura, diagramas e relatório), `scripts` (rotinas) e `.github/workflows` (CI).')
new()

H('7. Validação e aderência à rubrica')
tbl(['Critério','Peso','Evidência / condição'],[
('Planejamento','0,2','Não houve a 1ª entrega. Critério depende de avaliação docente.'),
('Pilares POO','0,3','Classe abstrata, interface, quatro subclasses, @Override e tipagem Java.'),
('CRUD','0,3','Create/Read/Update/Delete na entidade ocorrência; formulários HTTP.'),
('Banco de dados','0,2','`schema.sql`, implementações JDBC e teste integrado com PostgreSQL.'),
],[116,48,314])
S(18)
H2('Evidências de teste disponíveis')
P('Na revisão de 13/11/2026, foram aprovadas <b>35 verificações de domínio e CRUD</b> e <b>24 verificações HTTP</b>. O GitHub Actions também concluiu os testes JDBC com <b>PostgreSQL 16 real</b> e o teste ponta a ponta com inicialização da aplicação e operações CRUD pelo navegador HTTP. Evidência: <link href='https://github.com/pitado/planoB/actions/runs/43478898328' color='blue'>execução de CI aprovada</link>. O teste integrado não foi executado neste ambiente local.')
H2('Demonstração prática sugerida')
P('Iniciar banco com Docker, executar testes, cadastrar ocorrência, alterar status, criar e concluir ação, verificar histórico, excluir registro e conferir persistência e cascata no banco. Apresentar código das classes e os diagramas ao explicar as escolhas.')
H2('Pendências para entrega acadêmica')
P('1) Informar os integrantes e suas contribuições efetivas no cronograma. 2) Confirmar com o professor a avaliação de aderência, pois o 1º bimestre não foi entregue. 3) Executar uma demonstração local antes da apresentação.')

pdf=SimpleDocTemplate(str(OUT),pagesize=A4,rightMargin=20*mm,leftMargin=20*mm,topMargin=22*mm,bottomMargin=23*mm,title='Plano B - AEP 2026.2 | Documentação retrospectiva',author='Projeto Plano B (elaboração assistida)')
pdf.build(story,onFirstPage=footer,onLaterPages=footer)
print(OUT)
