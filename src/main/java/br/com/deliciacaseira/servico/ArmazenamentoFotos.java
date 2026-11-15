package br.com.deliciacaseira.servico;

import br.com.deliciacaseira.dominio.RegraNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Guarda as fotos dos produtos (RF22) em disco com segurança (corrige S7):
 * valida o conteúdo real do arquivo (assinatura JPEG/PNG/WEBP, não a extensão informada),
 * limita o tamanho e salva com nome aleatório gerado pelo sistema (sem path traversal).
 */
@Component
public class ArmazenamentoFotos {

    public static final long TAMANHO_MAXIMO = 3L * 1024 * 1024;
    private static final Pattern NOME_VALIDO = Pattern.compile("[0-9a-f\\-]{36}\\.(jpg|png|webp)");

    private final Path diretorio;

    public ArmazenamentoFotos(@Value("${app.uploads.dir}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public Path getDiretorio() { return diretorio; }

    public String salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw new RegraNegocioException("Escolha uma foto.");
        if (arquivo.getSize() > TAMANHO_MAXIMO) throw new RegraNegocioException("A foto deve ter no máximo 3 MB.");
        try {
            byte[] cabecalho;
            try (InputStream in = arquivo.getInputStream()) {
                cabecalho = in.readNBytes(12);
            }
            String extensao = detectarExtensao(cabecalho);
            if (extensao == null) throw new RegraNegocioException("Formato não aceito. Envie uma foto JPG, PNG ou WEBP.");
            Files.createDirectories(diretorio);
            String nome = UUID.randomUUID() + "." + extensao;
            try (InputStream in = arquivo.getInputStream()) {
                Files.copy(in, diretorio.resolve(nome), StandardCopyOption.REPLACE_EXISTING);
            }
            return nome;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar a foto.", e);
        }
    }

    public void apagar(String nome) {
        if (nome == null || !NOME_VALIDO.matcher(nome).matches()) return;
        try {
            Files.deleteIfExists(diretorio.resolve(nome));
        } catch (IOException ignorada) {
            // foto antiga que não pôde ser removida não impede a operação
        }
    }

    static String detectarExtensao(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 8 && Arrays.equals(Arrays.copyOf(b, 8),
                new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) return "png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }
}
