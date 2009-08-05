

import org.ptolemy3d.tool.batch.DemCutter;

/**
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class DemCutterTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String input = "./indata/ -102000000 41000000 102400 12800 00 ./outdata/ 1000000 .hdr .dat 1 0";
        String[] demArguments = input.split(" ");
        DemCutter.main(demArguments);
    }
}
