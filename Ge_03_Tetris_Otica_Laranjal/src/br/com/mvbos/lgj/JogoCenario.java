package br.com.mvbos.lgj;

import br.com.mvbos.lgj.base.CenarioPadrao;
import br.com.mvbos.lgj.base.Texto;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.util.Random;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class JogoCenario extends CenarioPadrao {

	enum Estado {
		JOGANDO, GANHOU, PERDEU
	}

	private static final int ESPACAMENTO = 2;

	private static final int ESPACO_VAZIO = -1;

	private static final int LINHA_COMPLETA = -2;

	private int largBloco, altBloco; // largura bloco e altura bloco

	private int ppx, ppy; // Posicao peca x e y

	private final int[][] grade = new int[10][16];

	private int temporizador = 0;

	private Texto texto = new Texto(20);

	private Random rand = new Random();

	private int idPeca = -1;
	private int idPrxPeca = -1;
	private Color corPeca;
	private int[][] peca;

	private int nivel = Jogo.nivel;
	private int pontos;
	private int linhasFeistas;

	private boolean animar;
	private boolean depurar;

	private Estado estado = Estado.JOGANDO;

	// Som
	private AudioInputStream as;

	private Clip clipAdicionarPeca;

	private Clip clipMarcarLinha;

	private Sequencer seqSomDeFundo;

	private boolean somParado;

	public JogoCenario(int largura, int altura) {
		super(largura, altura);
	}

	@Override
	public void carregar() {
		largBloco = largura / grade.length;
		altBloco = altura / grade[0].length;

		for (int i = 0; i < grade.length; i++) {
			for (int j = 0; j < grade[0].length; j++) {
				grade[i][j] = ESPACO_VAZIO;
			}
		}

		try {
			as = AudioSystem.getAudioInputStream(new File("som/adiciona_peca.wav"));
			clipAdicionarPeca = AudioSystem.getClip();
			clipAdicionarPeca.open(as);

			as = AudioSystem.getAudioInputStream(new File("som/109662_grunz_success.wav"));
			clipMarcarLinha = AudioSystem.getClip();
			clipMarcarLinha.open(as);

			seqSomDeFundo = MidiSystem.getSequencer();
			seqSomDeFundo.setSequence(MidiSystem.getSequence(new File("som/piano_quebrado.mid")));
			seqSomDeFundo.open();

			seqSomDeFundo.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);

			seqSomDeFundo.start();

		} catch (Exception e) {
			e.printStackTrace();
		}

		adicionaPeca();
	}

	@Override
	public void descarregar() {

		if (clipAdicionarPeca != null) {
			clipAdicionarPeca.stop();
			clipAdicionarPeca.close();
		}

		if (clipMarcarLinha != null) {
			clipMarcarLinha.stop();
			clipMarcarLinha.close();
		}

		if (seqSomDeFundo != null) {
			seqSomDeFundo.stop();
			seqSomDeFundo.close();
		}
	}

	private void pararSons() {
		if (somParado)
			return;

		somParado = true;

		if (clipAdicionarPeca != null)
			clipAdicionarPeca.stop();

		if (clipMarcarLinha != null)
			clipMarcarLinha.stop();

		if (seqSomDeFundo != null)
			seqSomDeFundo.stop();
	}

	@Override
	public void atualizar() {

		if (estado != Estado.JOGANDO) {
			return;
		}

		if (Jogo.controleTecla[Jogo.Tecla.ESQUERDA.ordinal()]) {
			if (validaMovimento(peca, ppx - 1, ppy))
				ppx--;

		} else if (Jogo.controleTecla[Jogo.Tecla.DIREITA.ordinal()]) {
			if (validaMovimento(peca, ppx + 1, ppy))
				ppx++;
		}

		if (Jogo.controleTecla[Jogo.Tecla.CIMA.ordinal()]) {
			girarReposicionarPeca(false);

		} else if (Jogo.controleTecla[Jogo.Tecla.BAIXO.ordinal()]) {
			if (validaMovimento(peca, ppx, ppy + 1))
				ppy++;
		}

		if (Jogo.controleTecla[Jogo.Tecla.BC.ordinal()]) {
			if (depurar) {
				if (++idPeca == Peca.PECAS.length)
					idPeca = 0;

				peca = Peca.PECAS[idPeca];
				corPeca = Peca.Cores[idPeca];

			} else {
				quedaRapida();
			}
		}

		Jogo.liberaTeclas();

		if (animar && temporizador >= 5) {
			animar = false;

			descerColunas();
			adicionaPeca();

		} else if (temporizador >= 20) {
			temporizador = 0;

			if (colidiu(ppx, ppy + 1)) {

				if (clipAdicionarPeca != null) {
					clipAdicionarPeca.setFramePosition(0);
					clipAdicionarPeca.start();
				}

				if (!parouForaDaGrade()) {
					adicionarPecaNaGrade();
					animar = marcarLinha();

					peca = null;

					if (!animar)
						adicionaPeca();

				} else {
					estado = Estado.PERDEU;
					pararSons();
				}

			} else
				ppy++;

		} else
			temporizador += nivel;

	}

	private int calculaPousoFantasma() {
		int y = ppy;

		while (validaMovimento(peca, ppx, y + 1)) {
			y++;
		}

		return y;
	}

	private void quedaRapida() {
		if (peca == null)
			return;

		while (validaMovimento(peca, ppx, ppy + 1)) {
			ppy++;
		}

		temporizador = 20;
	}

	public void adicionaPeca() {

		ppy = -2;
		ppx = grade.length / 2 - 1;

		// Primeira chamada
		if (idPeca == -1)
			idPeca = rand.nextInt(Peca.PECAS.length);
		else
			idPeca = idPrxPeca;
		// idPeca=6;
		idPrxPeca = rand.nextInt(Peca.PECAS.length);

		// Isso acontece muito
		if (idPeca == idPrxPeca)
			idPrxPeca = rand.nextInt(Peca.PECAS.length);

		peca = Peca.PECAS[idPeca];
		corPeca = Peca.Cores[idPeca];

	}

	private void adicionarPecaNaGrade() {

		for (int col = 0; col < peca.length; col++) {
			for (int lin = 0; lin < peca[col].length; lin++) {

				if (peca[lin][col] != 0) {

					grade[col + ppx][lin + ppy] = idPeca;

				}
			}
		}
	}

	private boolean validaMovimento(int[][] peca, int px, int py) {

		if (peca == null)
			return false;

		for (int col = 0; col < peca.length; col++) {
			for (int lin = 0; lin < peca[col].length; lin++) {
				if (peca[lin][col] == 0)
					continue;

				int prxPx = col + px; // Proxima posicao peca x
				int prxPy = lin + py; // Proxima posicao peca y

				if (prxPx < 0 || prxPx >= grade.length)
					return false;

				if (prxPy >= grade[0].length)
					return false;

				if (prxPy < 0)
					continue;

				// Colidiu com uma peca na grade
				if (grade[prxPx][prxPy] > ESPACO_VAZIO)
					return false;

			}
		}

		return true;
	}

	private boolean parouForaDaGrade() {

		if (peca == null)
			return false;

		for (int lin = 0; lin < peca.length; lin++) {
			for (int col = 0; col < peca[lin].length; col++) {
				if (peca[lin][col] == 0)
					continue;
				// Fora da grade
				if (lin + ppy < 0)
					return true;
			}
		}

		return false;
	}

	private boolean colidiu(int px, int py) {

		if (peca == null)
			return false;

		for (int col = 0; col < peca.length; col++) {
			for (int lin = 0; lin < peca[col].length; lin++) {
				if (peca[lin][col] == 0)
					continue;

				int prxPx = col + px;
				int prxPy = lin + py;

				if (depurar) {
					if (prxPx < 0 || prxPx >= grade.length)
						return false;
				}
				// Chegou na base da grade
				if (prxPy == grade[0].length)
					return true;

				// Fora da grade
				if (prxPy < 0)
					continue;

				// Colidiu com uma peca na grade
				if (grade[prxPx][prxPy] > ESPACO_VAZIO)
					return true;
			}
		}

		return false;
	}

	private boolean marcarLinha() {
		int multPontos = 0;

		for (int lin = grade[0].length - 1; lin >= 0; lin--) {
			boolean linhaCompleta = true;

			for (int col = grade.length - 1; col >= 0; col--) {
				if (grade[col][lin] == ESPACO_VAZIO) {
					linhaCompleta = false;
					break;
				}
			}

			if (linhaCompleta) {
				multPontos++;
				for (int col = grade.length - 1; col >= 0; col--) {
					grade[col][lin] = LINHA_COMPLETA;
				}
			}
		}

		pontos += multPontos * multPontos;
		linhasFeistas += multPontos;

		if (nivel == 9 && linhasFeistas >= 9) {
			estado = Estado.GANHOU;
			pararSons();

		} else if (linhasFeistas >= 9) {
			nivel++;
			linhasFeistas = 0;
		}

		return multPontos > 0;
	}

	private void descerColunas() {
		for (int col = 0; col < grade.length; col++) {
			for (int lin = grade[0].length - 1; lin >= 0; lin--) {

				if (grade[col][lin] == LINHA_COMPLETA) {
					int moverPara = lin;
					int prxLinha = lin - 1;

					for (; prxLinha > -1; prxLinha--) {
						if (grade[col][prxLinha] == LINHA_COMPLETA)
							continue;
						else
							break;

					}

					for (; moverPara > -1; moverPara--, prxLinha--) {

						if (prxLinha > -1)
							grade[col][moverPara] = grade[col][prxLinha];
						else
							grade[col][moverPara] = ESPACO_VAZIO;

					}
				}
			}
		}

		if (clipMarcarLinha != null) {
			clipMarcarLinha.setFramePosition(0);
			clipMarcarLinha.start();
		}

	}

	private void girarReposicionarPeca(boolean sentidoHorario) {
		if (peca == null)
			return;

		int tempPx = ppx;
		final int[][] tempPeca = new int[peca.length][peca.length];

		for (int i = 0; i < peca.length; i++) {
			for (int j = 0; j < peca.length; j++) {
				if (sentidoHorario)
					tempPeca[j][peca.length - i - 1] = peca[i][j];
				else
					tempPeca[peca.length - j - 1][i] = peca[i][j];
			}
		}

		// Reposiciona peca na tela
		for (int i = 0; i < tempPeca.length; i++) {
			for (int j = 0; j < tempPeca.length; j++) {
				if (tempPeca[j][i] == 0) {
					continue;
				}

				int prxPx = i + tempPx;

				if (prxPx < 0)
					tempPx = tempPx - prxPx;

				else if (prxPx == grade.length)
					tempPx = tempPx - 1;

			}
		}

		if (validaMovimento(tempPeca, tempPx, ppy)) {
			peca = tempPeca;
			ppx = tempPx;
		}
	}

	@Override
	public void desenhar(Graphics2D g) {

		g.setColor(new Color(40, 40, 40));

		for (int col = 0; col <= grade.length; col++) {
			int x = col * largBloco;
			g.drawLine(x, 0, x, grade[0].length * altBloco);
		}

		for (int lin = 0; lin <= grade[0].length; lin++) {
			int y = lin * altBloco;
			g.drawLine(0, y, grade.length * largBloco, y);
		}

		g.setColor(Color.GRAY);
		g.drawRect(0, 0, grade.length * largBloco - 1, grade[0].length * altBloco - 1);

		for (int col = 0; col < grade.length; col++) {
			for (int lin = 0; lin < grade[0].length; lin++) {
				int valor = grade[col][lin];

				if (valor == ESPACO_VAZIO)
					continue;

				if (valor == LINHA_COMPLETA)
					g.setColor(Color.RED);
				else
					g.setColor(Peca.Cores[valor]);

				int x = col * largBloco + ESPACAMENTO;
				int y = lin * altBloco + ESPACAMENTO;

				g.fillRect(x, y, largBloco - ESPACAMENTO, altBloco - ESPACAMENTO);

			}
		}

		if (peca != null && estado == Estado.JOGANDO) {
			int ppyFantasma = calculaPousoFantasma();

			if (ppyFantasma != ppy) {
				Color corFantasma = new Color(corPeca.getRed(), corPeca.getGreen(), corPeca.getBlue(), 70);
				g.setColor(corFantasma);

				for (int col = 0; col < peca.length; col++) {
					for (int lin = 0; lin < peca[col].length; lin++) {
						if (peca[lin][col] == 0)
							continue;

						int x = (col + ppx) * largBloco + ESPACAMENTO;
						int y = (lin + ppyFantasma) * altBloco + ESPACAMENTO;

						g.fillRect(x, y, largBloco - ESPACAMENTO, altBloco - ESPACAMENTO);
					}
				}
			}
		}

		if (peca != null) {
			g.setColor(corPeca);

			for (int col = 0; col < peca.length; col++) {
				for (int lin = 0; lin < peca[col].length; lin++) {
					if (peca[lin][col] != 0) {

						int x = (col + ppx) * largBloco + ESPACAMENTO;
						int y = (lin + ppy) * altBloco + ESPACAMENTO;

						g.fillRect(x, y, largBloco - ESPACAMENTO, altBloco - ESPACAMENTO);

					} else if (depurar) {
						g.setColor(Color.PINK);
						int x = (col + ppx) * largBloco + ESPACAMENTO;
						int y = (lin + ppy) * altBloco + ESPACAMENTO;

						g.fillRect(x, y, largBloco - ESPACAMENTO, altBloco - ESPACAMENTO);

						g.setColor(corPeca);
					}
				}
			}
		}

		int miniatura = largBloco / 4;
		int[][] prxPeca = Peca.PECAS[idPrxPeca];
		g.setColor(Peca.Cores[idPrxPeca]);

		for (int col = 0; col < prxPeca.length; col++) {
			for (int lin = 0; lin < prxPeca[col].length; lin++) {
				if (prxPeca[lin][col] == 0)
					continue;

				int x = col * miniatura + ESPACAMENTO;
				int y = lin * miniatura + ESPACAMENTO;

				g.fillRect(x, y, miniatura - ESPACAMENTO, miniatura - ESPACAMENTO);

			}
		}

		// Logomarca centralizada abaixo do placar
		texto.setCor(Color.YELLOW);
		texto.desenha(g, "TETRIS  OTICA LARANJAL", largura / 2 - 120, 50);

		texto.setCor(Color.WHITE);
		texto.desenha(g, "Level " + nivel + " - " + linhasFeistas, largura / 2 - 20, 20);
		texto.desenha(g, String.valueOf(pontos), largura - 50, 20);

		if (estado != Estado.JOGANDO) {
			texto.setCor(Color.WHITE);

			if (estado == Estado.GANHOU)
				texto.desenha(g, "Finalmente!", 180, 180);
			else
				texto.desenha(g, "Deu ruim!", 180, 180);
		}
	}

}
