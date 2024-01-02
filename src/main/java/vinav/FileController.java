package vinav;

import com.itextpdf.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class FileController {

    @Autowired
    FilesStorageService storageService;

    @Autowired
    ExtractService extractService;

    @GetMapping("/")
    public String homepage() {
        return "login";
    }

    @PostMapping("/postlogin")
    public String homepost(@RequestParam("username") String username,@RequestParam("password") String password) {
        if(username.equals("mladmin")&&password.equals("edition"))
        return "redirect:/files";
        else
            return "login";
    }

    @GetMapping("/files/new")
    public String newFile(Model model) {
        return "upload_form";
    }

    @PostMapping("/files/upload")
    public String uploadFile(Model model, @RequestParam("file") MultipartFile file) {
        String message = "";
        try {
            storageService.save(file);
            message = "Uploaded the file successfully: " + file.getOriginalFilename();
            model.addAttribute("message", message);
        } catch (Exception e) {
            message = "Could not upload the file: " + file.getOriginalFilename() + ". Error: " + e.getMessage();
            model.addAttribute("message", message);
        }
        return "upload_form";
    }


    @GetMapping("/files")
    public String getListFiles(Model model) {
        List<FileInfo> fileInfos = storageService.loadAll()
                .filter(path -> !path.getFileName().toString().endsWith(".mp3"))
                .map(path -> {
                    String filename = path.getFileName().toString();
                    String url = MvcUriComponentsBuilder
                            .fromMethodName(FileController.class, "getFile", filename).build().toString();

                    return new FileInfo(filename, url);
                })
                .collect(Collectors.toList());


        model.addAttribute("files", fileInfos);
        return "files";
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource file = storageService.load(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @GetMapping("/files/delete/{filename:.+}")
    public String deleteFile(@PathVariable String filename, Model model, RedirectAttributes redirectAttributes) {
        try {
            boolean existed = storageService.delete(filename);
            if (existed) {
                redirectAttributes.addFlashAttribute("message", "Delete the file successfully: " + filename);
            } else {
                redirectAttributes.addFlashAttribute("message", "The file does not exist!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message",
                    "Could not delete the file: " + filename + ". Error: " + e.getMessage());
        }
        return "redirect:/files";
    }


    @GetMapping("/files/translate/language")
    public String translateFile(@RequestParam("language") String language,
                                @RequestParam("name") String fileName,
                                @RequestParam("destinationLanguage") String destinationLanguage,
                                Model model) throws IOException, DocumentException, InterruptedException {
        System.out.println(language + " for "+fileName);
        String fileData = extractService.getTxtFromAwsForFile(fileName, language, destinationLanguage);
        System.out.println("Back to file controller"+fileData);
        model.addAttribute("translatedText", fileData);
        return "download";
    }

    @GetMapping("/files/braille/{filename:.+}")
    public String translateToBraille(@PathVariable String filename, Model model) throws IOException, DocumentException, InterruptedException {
        String fileData = extractService.getTxtFromAwsForFile(filename, "braille", null);
        System.out.println("Back to file controller" + fileData);
        model.addAttribute("translatedText", fileData);
        return "download";
    }

    @GetMapping("/files/analyze/{filename:.+}")
    public String analyse(@PathVariable String filename, Model model) throws IOException, DocumentException, InterruptedException {
        String fileData = extractService.getAnalysis(filename);


        String[] words = fileData.split("\\s+"); // Split by whitespace

        // Create a set to store unique words
        Set<String> uniqueWordsSet = new HashSet<>();

        // Iterate through the words, adding each unique word to the set
        for (String word : words) {
            uniqueWordsSet.add(word);
        }

// Convert the set to a string with words separated by a space
        String uniqueWordsString = String.join(", ", uniqueWordsSet);
        uniqueWordsString = uniqueWordsString.toLowerCase();
        model.addAttribute("uniqueWordsString", uniqueWordsString);
        return "words";
    }


    @GetMapping("/files/translate/foreign")
    public String foreign(@RequestParam("forlanguage") String forlanguage,
                                @RequestParam("name") String fileName,
                                Model model) throws IOException, DocumentException, InterruptedException {
        return "foreign";
    }

    @GetMapping("/files/demo")
    public String demoSound()  {
        return "demo";
    }

}


