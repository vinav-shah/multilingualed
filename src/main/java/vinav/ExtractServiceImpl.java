package vinav;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;
import com.amazonaws.services.textract.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ExtractServiceImpl implements ExtractService{

    @Value("${clients.S3BucketAccessKey}")
    private String s3BucketAccessKey;

    @Value("${clients.S3SecretKey}")
    private String s3SecretKey;

    @Value("${clients.S3BucketName}")
    private String s3BucketName;

    @Value("${clients.S3UploadPath}")
    private String s3UploadPath;

    private final Path root = Paths.get("./upload");

    private static final Region REGION = Region.US_EAST_2;


   /* public String getAnalysis(String filename) throws IOException, InterruptedException {

        //AWS Related
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(s3BucketAccessKey, s3SecretKey);
        // Create an S3 client
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion("us-east-2").build();
        // Upload a file to S3
        uploadFile(s3Client, s3BucketName, filename, s3UploadPath + "/" + filename);
z
        // Retrieve text content from the uploaded file using textract
        StringBuffer fileContent = retrieveFileContent(s3Client, s3BucketName, filename, awsCredentials,null);
        System.out.println("File Content:\n" + fileContent);

        return fileContent.toString();
    }*/

    @Override
    public String getTxtFromAwsForFile(String filename, String language, String destLang) throws IOException, InterruptedException {

            //AWS Related
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(s3BucketAccessKey, s3SecretKey);
            // Create an S3 client
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .withRegion("us-east-2").build();
            // Upload a file to S3
            uploadFile(s3Client, s3BucketName, filename, s3UploadPath + "/" + filename);

        // Retrieve text content from the uploaded file using textract
        StringBuffer fileContent = retrieveFileContent(s3Client, s3BucketName, filename, awsCredentials, language, destLang);
        System.out.println("File Content:\n" + fileContent);

        return fileContent.toString();
    }

    @Override
    public String getAnalysis(String filename) throws IOException, InterruptedException {
        //AWS Related
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(s3BucketAccessKey, s3SecretKey);
        // Create an S3 client
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion("us-east-2").build();
        // Upload a file to S3
        uploadFile(s3Client, s3BucketName, filename, s3UploadPath + "/" + filename);

        // Retrieve text content from the uploaded file using textract
        StringBuffer fileContent = retrieveFileContent(s3Client, s3BucketName, filename, awsCredentials, null,null);
        System.out.println("File Content:\n" + fileContent);

        return fileContent.toString();
    }


    private static final int MAX_TRANSLATE_SIZE = 9000;  // Using 9000 bytes to keep a safe margin below the 10,000 byte limit.

    private StringBuffer retrieveFileContent(AmazonS3 s3Client, String bucketName, String objectKey, BasicAWSCredentials awsCredentials, String language, String destLang) throws InterruptedException {
        AmazonTextract textractClient = AmazonTextractClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion("us-east-2")
                .build();

        S3Object s3Object = new S3Object().withBucket(bucketName).withName(objectKey);
        DocumentLocation documentLocation = new DocumentLocation().withS3Object(s3Object);
        StartDocumentTextDetectionRequest request = new StartDocumentTextDetectionRequest().withDocumentLocation(documentLocation);

        String jobId = null;
        StartDocumentTextDetectionResult startResult = null;
/*
        if(objectKey.equals("emperorsclothes.pdf")){
            jobId = "0c332404e30e3c780d25bfcdd4372e633bf0d9cdc27637de70e3d44b94a92265";
        }
        else {
*/
            startResult=textractClient.startDocumentTextDetection(request);
            jobId = startResult.getJobId();
/*        }*/
        System.out.println(jobId);
        String status;
        GetDocumentTextDetectionResult textDetectionResult;
        StringBuffer detectedTextBuffer = new StringBuffer();
        String nextToken = null;

        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            textDetectionResult = textractClient.getDocumentTextDetection(new GetDocumentTextDetectionRequest().withJobId(jobId));
            status = textDetectionResult.getJobStatus();
            if ("SUCCEEDED".equals(status) || "FAILED".equals(status)) {
                break;
            }
        }

        if ("SUCCEEDED".equals(status)) {
            Set<String> uniqueTextBlocks = new HashSet<>(); // To store unique text blocks
            do {
                textDetectionResult = textractClient.getDocumentTextDetection(new GetDocumentTextDetectionRequest()
                        .withJobId(jobId)
                        .withNextToken(nextToken));

                for (Block block : textDetectionResult.getBlocks()) {
                    if (BlockType.LINE.toString().equals(block.getBlockType())) {
                        String blockText = block.getText();
                        if (!uniqueTextBlocks.contains(blockText)) {
                            detectedTextBuffer.append(blockText).append(" ");
                            uniqueTextBlocks.add(blockText); // Add to set to avoid duplicates
                        }
                    }
                }
                nextToken = textDetectionResult.getNextToken();
            } while (nextToken != null && !nextToken.trim().isEmpty());
        } else {
            System.out.println("Job failed");
        }

        //Translations
        String textToTranslate = detectedTextBuffer.toString();
        System.out.println("detectedTextBuffer"+detectedTextBuffer);
        StringBuffer translatedTextBuffer = new StringBuffer();

        if(language==null) {
            String content = textToTranslate; // Replace with your input content
            int[] lineNum = {1};
            List<Tok> tokens = Lex.getTokens(content, lineNum);
            // Set flags here (showAll, lident, nintcon, lscon)
            boolean showAll = true;
            boolean lident = true;
            boolean nintcon = true;
            boolean lscon = true;

            translatedTextBuffer = Lex.formatResults(tokens, showAll, lident, nintcon, lscon, lineNum[0]);
            //remove duplicates
            translatedTextBuffer = removeDuplicates(translatedTextBuffer);
            return translatedTextBuffer;
        }

        //braille
        if("braille".equals(language)){
            return stringToBraille(textToTranslate);
        }

        //languages
        // Translate in chunks


        AwsBasicCredentials awsCredentials2 = AwsBasicCredentials.create(awsCredentials.getAWSAccessKeyId(), awsCredentials.getAWSSecretKey());

        TranslateClient translateClient = TranslateClient.builder()
                .region(Region.US_EAST_1) // Replace with your desired AWS region
                .credentialsProvider(() -> awsCredentials2)
                .build();

        int startPos = 0;
        while (startPos < textToTranslate.length()) {
            Thread.sleep(3000);//cant make so many calls
            int endPos = startPos + MAX_TRANSLATE_SIZE < textToTranslate.length() ? startPos + MAX_TRANSLATE_SIZE : textToTranslate.length();
            String chunk = textToTranslate.substring(startPos, endPos);
            TranslateTextRequest translateRequest = TranslateTextRequest.builder()
                    .sourceLanguageCode(language)  // Replace with your source language code
                    .targetLanguageCode(destLang)  // Replace with your target language code
                    .text(chunk)
                    .build();
            String translatedChunk = translateClient.translateText(translateRequest).translatedText();
            translatedTextBuffer.append(translatedChunk);
            startPos = endPos;
        }

        System.out.println("Translated text:\n" + translatedTextBuffer.toString());
        return translatedTextBuffer;
    }

    /*
     * Basics
     */
    private StringBuffer stringToBraille(String text) {

         String Braille[] = {
                "⠁", "⠃", "⠉", "⠙", "⠑", "⠋", "⠛", "⠓", "⠊",
                "⠚", "⠅", "⠇", "⠍", "⠝", "⠕", "⠏", "⠟", "⠗",
                "⠎", "⠞", "⠥", "⠧", "⠺", "⠭", "⠽", "⠵"
        };

         Map brailleMap = new HashMap<String, String>();
         brailleMap.put("a", "⠁");
        brailleMap.put("b", "⠃");
        brailleMap.put("c", "⠉");
        brailleMap.put("d", "⠙");
        brailleMap.put("e", "⠑");
        brailleMap.put("f","⠋");
        brailleMap.put("g", "⠛");
        brailleMap.put("h", "⠓");
        brailleMap.put("i", "⠊");
        brailleMap.put("j", "⠚");
        brailleMap.put("k", "⠅");
        brailleMap.put("l", "⠇");
        brailleMap.put("m", "⠍");
        brailleMap.put("n", "⠝");
        brailleMap.put("o", "⠕");
        brailleMap.put("p", "⠏");
        brailleMap.put("q", "⠟");
        brailleMap.put("r", "⠗");
        brailleMap.put("s", "⠎");
        brailleMap.put("t", "⠞");
        brailleMap.put("u", "⠥");
        brailleMap.put("v", "⠧");
        brailleMap.put("w", "⠺");
        brailleMap.put("x", "⠭");
        brailleMap.put("y", "⠽");
        brailleMap.put("z", "⠵");
        brailleMap.put("1", "⠼⠁");
        brailleMap.put("2", "⠼⠃");
        brailleMap.put("3", "⠼⠉");
        brailleMap.put("4", "⠼⠙");
        brailleMap.put("5", "⠼⠑");
        brailleMap.put("6", "⠼⠋");
        brailleMap.put("7", "⠼⠛");
        brailleMap.put("8", "⠼⠓");
        brailleMap.put("9", "⠼⠊");
        brailleMap.put("0", "⠼⠚");
        brailleMap.put(" ", " ");

        text = text.toLowerCase();
        StringBuffer result = new StringBuffer();

        StringBuffer sb=new StringBuffer();
        for (int i = 0; i <text.length(); i++) {
            // sb.append(text.substring(i, i+1)); //0,1
            String brailleLetterStr = text.substring(i, i+1);
            String brailleLetter = (String) brailleMap.get(brailleLetterStr);
            if(brailleLetter!=null){
            sb.append(brailleLetter);
            }
            else {
                sb.append(System.lineSeparator());
            }
        }
        System.out.println(sb.toString());
        result.append(sb);
        return sb;
    }

    private static void uploadFile(AmazonS3 s3Client, String bucketName, String objectKey, String filePath) {
        s3Client.putObject(bucketName, objectKey, new java.io.File(filePath));
        System.out.println("File uploaded successfully."+objectKey);
    }

    private static TranslateTextResponse translate(TranslateClient client, String sourceText, String targetLanguage) {
        TranslateTextRequest request = TranslateTextRequest.builder()
                .sourceLanguageCode("en")
                .targetLanguageCode(targetLanguage)
                .text(sourceText)
                .build();
        return client.translateText(request);
    }

    public static StringBuffer removeDuplicates(StringBuffer inputBuffer) {
        StringBuffer resultBuffer = new StringBuffer(inputBuffer.toString());
        int startIndex = 0;
        List<String> sentences = new ArrayList<>();

        while (startIndex < resultBuffer.length()) {
            int firstIndex = resultBuffer.indexOf("\"", startIndex);

            if (firstIndex == -1) {
                break; // No more double quotes found, exit the loop
            }

            int secondIndex = resultBuffer.indexOf("\"", firstIndex + 1);

            if (secondIndex == -1) {
                break; // No closing double quote found, exit the loop
            }

            String sentence = resultBuffer.substring(firstIndex, secondIndex + 1);

            if (!sentences.contains(sentence)) {
                sentences.add(sentence);
                startIndex = secondIndex + 1;
            } else {
                // Remove duplicate sentence
                resultBuffer.delete(firstIndex, secondIndex + 1);
            }
        }

        return resultBuffer;
    }



}
