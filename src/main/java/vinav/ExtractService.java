package vinav;
import java.io.IOException;

public interface ExtractService {
    public String getTxtFromAwsForFile(String fileName, String srclang, String destlang) throws IOException, InterruptedException;
    public String getAnalysis(String fileName) throws IOException, InterruptedException;
}
