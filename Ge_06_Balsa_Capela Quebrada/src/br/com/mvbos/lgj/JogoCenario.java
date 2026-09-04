package br.com.mvbos.lgj;

import br.com.mvbos.lgj.base.CenarioPadrao;
import br.com.mvbos.lgj.base.Texto;
import br.com.mvbos.lgj.base.Util;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.util.Random;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class JogoCenario extends CenarioPadrao {

	public enum Estado {
		JOGANDO, GANHOU, PERDEU
	}

	public static final int PONTOS_VITORIA = 300;

	public static final int DURACAO_FIM_DE_JOGO = 55; // ~2,5s a ~20 quadros/s

	private Nave nave;
	private Tiro[] tiros = new Tiro[25];
	private Asteroide[] aerolitos = new Asteroide[50];

	private Texto texto = new Texto();
	private Random rand = new Random();
	private Estado estado = Estado.JOGANDO;
	private int temporizadorFimDeJogo;

	private int pontos;
	private int adiciona = 2;
	private int contadorTiro;
	private int intervalo = 60;
	private int temporizador = 0;

	private float graus;

	// Som
	private Clip clipTiro;

	private Clip clipExplosao;

	private Clip clipVitoria;

	private Sequencer seqSomDeFundo;

	public JogoCenario(int largura, int altura) {
		super(largura, altura);
	}

	@Override
	public void carregar() {

		texto.setCor(Color.WHITE);

		for (int i = 0; i < tiros.length; i++) {
			tiros[i] = new Tiro(5, 5);
			tiros[i].setVel(5);
		}

		for (int i = 0; i < aerolitos.length; i++) {
			aerolitos[i] = new Asteroide();
		}

		nave = new Nave(0, 0, 40, 40);
		nave.setAtivo(true);
		nave.setCor(Color.YELLOW);

		Util.centraliza(nave, largura, altura);

		try {
			clipTiro = AudioSystem.getClip();
			clipTiro.open(AudioSystem.getAudioInputStream(new File("som/tiro.wav")));

			clipExplosao = AudioSystem.getClip();
			clipExplosao.open(AudioSystem.getAudioInputStream(new File("som/explosao.wav")));

			clipVitoria = AudioSystem.getClip();
			clipVitoria.open(AudioSystem.getAudioInputStream(new File("som/levelup.wav")));

			seqSomDeFundo = MidiSystem.getSequencer();
			seqSomDeFundo.setSequence(MidiSystem.getSequence(new File("som/fundo.mid")));
			seqSomDeFundo.open();
			seqSomDeFundo.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
			seqSomDeFundo.start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void descarregar() {
		nave = null;
		tiros = null;
		aerolitos = null;

		if (clipTiro != null) {
			clipTiro.stop();
			clipTiro.close();
		}

		if (clipExplosao != null) {
			clipExplosao.stop();
			clipExplosao.close();
		}

		if (clipVitoria != null) {
			clipVitoria.stop();
			clipVitoria.close();
		}

		if (seqSomDeFundo != null) {
			seqSomDeFundo.stop();
			seqSomDeFundo.close();
		}
	}

	private void tocar(Clip clip) {
		if (clip == null)
			return;

		clip.stop();
		clip.setFramePosition(0);
		clip.start();
	}

	@Override
	public void atualizar() {

		if (estado != Estado.JOGANDO) {
			temporizadorFimDeJogo++;
			return;
		}

		if (nave.getLargura() < 5) {
			estado = Estado.PERDEU;
			if (seqSomDeFundo != null)
				seqSomDeFundo.stop();
			return;
		}

		if (pontos >= PONTOS_VITORIA) {
			estado = Estado.GANHOU;
			if (seqSomDeFundo != null)
				seqSomDeFundo.stop();
			tocar(clipVitoria);
			return;
		}

		if (temporizador == intervalo) {
			temporizador = 0;
			maisAerolitos();

		} else
			temporizador++;

		if (Jogo.controleTecla[Jogo.Tecla.ESQUERDA.ordinal()])
			graus -= 10;
		else if (Jogo.controleTecla[Jogo.Tecla.DIREITA.ordinal()])
			graus += 10;

		if (graus < 0)
			graus += 360;
		else if (graus > 360)
			graus -= 360;

		nave.setAngulo(graus);

		if (Jogo.controleTecla[Jogo.Tecla.BC.ordinal()]) {
			adicionarTiro(nave.getAngulo());
			Jogo.liberaTecla(Jogo.Tecla.BC);
		}

		for (Asteroide ast : aerolitos) {

			if (!ast.isAtivo())
				continue;

			// Asteroides comecam fora da tela
			if (ast.getPy() < ast.getAltura() * -2 || ast.getPy() > ast.getAltura() + altura) {
				ast.setAtivo(false);
				continue;
			}

			if (ast.getPx() + ast.getLargura() < 0 || ast.getPx() > largura) {
				ast.setAtivo(false);
				continue;
			}

			for (Tiro tiro : tiros) {
				if (Util.colide(ast, tiro)) {
					ast.setAtivo(false);
					tiro.setAtivo(false);
					pontos += ast.getVel();
					tocar(clipExplosao);
					break;
				}
			}

			if (Util.colide(ast, nave)) {
				ast.setAtivo(false);
				nave.setLargura(nave.getLargura() - 2);
				nave.setAltura(nave.getAltura() - 2);
				nave.inverteCor();
				tocar(clipExplosao);

				Util.centraliza(nave, largura, altura);
				continue;
			}

			ast.atualiza();
		}

		for (Tiro tiro : tiros) {
			if (!tiro.isAtivo())
				continue;

			if (Util.saiu(tiro, largura, altura))
				tiro.setAtivo(false);
			else
				tiro.atualiza();
		}

	}

	private void adicionarTiro(float angulo) {
		if (contadorTiro > 1)
			contadorTiro--;
		else
			contadorTiro = tiros.length - 1;

		Tiro t = tiros[contadorTiro];

		t.setAngulo(angulo);
		t.setPx(nave.getPx() + nave.getLargura() / 2 - t.getLargura() / 2);
		t.setPy(nave.getPy() + nave.getAltura() / 2 - t.getAltura() / 2);

		t.setAtivo(true);

		tocar(clipTiro);
	}

	private void maisAerolitos() {
		int contador = 0;
		for (int i = 0; i < aerolitos.length; i++) {
			if (contador == adiciona)
				break;

			Asteroide ast = aerolitos[i];

			if (ast.isAtivo())
				continue;

			contador++;
			ast.setAtivo(true);

			ast.setAltura((rand.nextInt(4) + 1) * 10);
			ast.setLargura(ast.getAltura());

			ast.setPx(rand.nextInt(largura));

			int py = rand.nextInt(2) * altura;
			if (py == 0)
				py = py - ast.getAltura();

			ast.setPy(py);
			ast.setVel(rand.nextInt(3) + 1);

			switch (Jogo.nivel) {
			case 0:
				// Modo Facil: Vai para qualquer lado
				ast.setAngulo(rand.nextInt(360));
				break;
			case 1:
				// Modo Normal: Vai em angulos proximos a nave do jogador
				if (ast.getPy() <= 0)
					ast.setAngulo(90);
				else
					ast.setAngulo(270);
				break;
			case 2:
				// Modo Dificil: Todos vao em diracao a nave do jogador
				float arco = (float) Math.atan2(nave.getPy() - ast.getPy(), nave.getPx() - ast.getPx());
				float angulo = (float) Math.toDegrees(arco);
				ast.setAngulo(angulo);
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void desenhar(Graphics2D g) {
		texto.desenha(g, "GE TAVARES | Pontos: " + pontos + " / " + PONTOS_VITORIA, 10, 20);

		for (int i = 0; i < tiros.length; i++) {
			if (tiros[i].isAtivo())
				tiros[i].desenha(g);
		}

		for (int i = 0; i < aerolitos.length; i++) {
			if (aerolitos[i].isAtivo())
				aerolitos[i].desenha(g);
		}

		nave.desenha(g);

		if (estado != Estado.JOGANDO) {
			texto.desenha(g, estado == Estado.GANHOU ? "VOCE VENCEU!" : "GAME OVER", largura / 2 - 70, altura / 2);
			texto.desenha(g, "Voltando ao menu...", largura / 2 - 80, altura / 2 + 25);
			texto.desenha(g, "GE TAVARES", largura / 2 - 50, altura / 2 + 50);
		}
	}

	public boolean deveVoltarAoMenu() {
		return estado != Estado.JOGANDO && temporizadorFimDeJogo >= DURACAO_FIM_DE_JOGO;
	}

}
