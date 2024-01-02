package vinav;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum Token {
    PRINT, PRINTLN, REPEAT, BEGIN, END,
    IDENT,
    ICONST, SCONST,
    PLUS, MINUS, STAR, SLASH, EQ, LPAREN, RPAREN, SC,
    ERR,
    DONE
}

class Tok {
    private Token token;
    private String lexeme;
    private int lnum;

    public Tok() {
        token = Token.ERR;
        lnum = -1;
    }

    public Tok(Token token, String lexeme, int line) {
        this.token = token;
        this.lexeme = lexeme;
        this.lnum = line;
    }

    public boolean equals(Token token) {
        return this.token == token;
    }

    public boolean notEquals(Token token) {
        return this.token != token;
    }

    public Token getToken() {
        return token;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLinenum() {
        return lnum;
    }

    @Override
    public String toString() {
        return lexeme + ": " + token + ", line " + lnum;
    }
}

public class Lex {
    private static final int npos = -1;

    public static List<Tok> signConverter(List<Tok> oldList) {
        for (int i = 0; i < oldList.size(); i++) {
            if (oldList.get(i).equals(Token.MINUS) || oldList.get(i).equals(Token.PLUS)) {
                if ((i + 1) < oldList.size() && oldList.get(i + 1).equals(Token.ICONST) &&
                        oldList.get(i + 1).getLinenum() == oldList.get(i).getLinenum()) {
                    if (i == 0 || oldList.get(i - 1).getLinenum() < oldList.get(i).getLinenum() ||
                            oldList.get(i - 1).equals(Token.EQ) || oldList.get(i - 1).equals(Token.PLUS) ||
                            oldList.get(i - 1).equals(Token.MINUS) || oldList.get(i - 1).equals(Token.STAR) ||
                            oldList.get(i - 1).equals(Token.SLASH)) {
                        oldList.set(i + 1, new Tok(oldList.get(i + 1).getToken(),
                                oldList.get(i).getLexeme() + oldList.get(i + 1).getLexeme(),
                                oldList.get(i + 1).getLinenum()));
                        oldList.remove(i);
                    }
                }
            }
        }
        return oldList;
    }

    public static StringBuffer printByToken(List<Tok> fullList, Token t,StringBuffer sb) {
        //String separator = "\n";
        int count = 0;

        for (Tok element : fullList) {
            if (element.getToken().equals(t)) {
                count++;
                    System.out.print( element.getLexeme());
                    sb.append( element.getLexeme()+"\s");

            }
        }

        return sb;
    }

    public static StringBuffer formatResults(List<Tok> resultingList, boolean showAll, boolean lident, boolean nintcon, boolean lscon, int lineNum) {

        StringBuffer sb = new StringBuffer();

        if (showAll) {
            for (Tok element : resultingList) {
                System.out.println(element);
                //sb.append(element.getLexeme());
            }
        }

        System.out.println("\n Lines: " + lineNum);
        //sb.append("\n Lines: " + lineNum);

        if (lineNum != 0) {
            int numTokens = resultingList.size();
            if (resultingList.get(resultingList.size() - 1).getToken() == Token.DONE) {
                numTokens--;
            }

            //System.out.println("\n Tokens: " + numTokens);
            //sb.append("\n Tokens: " + numTokens +"\n");


            if (lident || nintcon || lscon) {
                resultingList.sort((a, b) -> a.getLexeme().compareTo(b.getLexeme()));

                if (lident) {
                    //System.out.print("\n IDENTIFIERS: \n");
                    //sb.append("\n\n IDENTIFIERS in the book: \n");
                    sb = printByToken(resultingList, Token.IDENT,sb);
                }

                if (nintcon) {
                    //System.out.print("\nINTEGERS: \n");
                    //sb = printByToken(resultingList, Token.ICONST, sb);
                }

                if (lscon) {
                    //System.out.print("\n\n Quotes used in the book : \n\n");
                    //sb.append("\n\n Quotes used in the book : \n");
                    //sb = printByToken(resultingList, Token.SCONST,sb);
                }
            }
        }
        return sb;
    }

    public static String removeComments(String content, int lineNum) {
        String comments = "//";

        if (content.length() >= 2) {
            if (content.substring(0, 2).equals(comments)) {
                if (content.indexOf('\n') == -1) {
                    return "RETURN";
                } else {
                    Pattern pattern = Pattern.compile("//(.*?)\\n");
                    Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        content = content.substring(matcher.end(), npos);
                    }
                }
            } else {
                return "NONE";
            }
        } else {
            return "NONE";
        }
        return content;
    }

    public static List<Tok> getTokens(String content, int[] lineNum) {
        List<Tok> tokenize = new ArrayList<>();
        Map<Token, Pattern> matchMe = new HashMap<>();

        matchMe.put(Token.PRINTLN, Pattern.compile("PRINTLN", Pattern.CASE_INSENSITIVE));
        matchMe.put(Token.PRINT, Pattern.compile("PRINT", Pattern.CASE_INSENSITIVE));
        matchMe.put(Token.REPEAT, Pattern.compile("REPEAT", Pattern.CASE_INSENSITIVE));
        matchMe.put(Token.BEGIN, Pattern.compile("BEGIN", Pattern.CASE_INSENSITIVE));
        matchMe.put(Token.END, Pattern.compile("END", Pattern.CASE_INSENSITIVE));
        matchMe.put(Token.ICONST, Pattern.compile("[+-]?[0-9]+"));
        matchMe.put(Token.SCONST, Pattern.compile("\"(.*?)\""));
        matchMe.put(Token.PLUS, Pattern.compile("\\+"));
        matchMe.put(Token.MINUS, Pattern.compile("-"));
        matchMe.put(Token.STAR, Pattern.compile("\\*"));
        matchMe.put(Token.SLASH, Pattern.compile("/"));
        matchMe.put(Token.EQ, Pattern.compile("="));
        matchMe.put(Token.LPAREN, Pattern.compile("\\("));
        matchMe.put(Token.RPAREN, Pattern.compile("\\)"));
        matchMe.put(Token.SC, Pattern.compile(";"));
        matchMe.put(Token.DONE, Pattern.compile("DONE", Pattern.CASE_INSENSITIVE));
        matchMe.put(Token.IDENT, Pattern.compile("[a-zA-Z][0-9a-zA-Z_]*"));

        while (!content.isEmpty()) {
            String remComResult = removeComments(content, lineNum[0]);

            if (remComResult.equals("NONE")) {
            } else if (remComResult.equals("RETURN")) {
                tokenize = signConverter(tokenize);
                return tokenize;
            } else {
                lineNum[0]++;
                continue;
            }

            for (Token curToken : Arrays.asList(Token.PRINTLN, Token.PRINT, Token.REPEAT, Token.BEGIN, Token.END, Token.SC,
                    Token.DONE, Token.SCONST, Token.PLUS, Token.MINUS, Token.STAR, Token.SLASH, Token.EQ, Token.LPAREN,
                    Token.RPAREN, Token.ICONST, Token.IDENT)) {
                Matcher matcher = matchMe.get(curToken).matcher(content);
                if (matcher.find() && content.indexOf(matcher.group(0)) == 0) {
                    tokenize.add(new Tok(curToken, matcher.group(0), lineNum[0]));
                    content = content.substring(matcher.group(0).length());
                    break;
                }
                if (curToken.equals(Token.IDENT)) {
                    String firstChar = content.substring(0, 1);
                    if (!firstChar.equals("\n") && !firstChar.equals(" ") && !firstChar.equals("\t")) {
                        tokenize.add(new Tok(Token.ERR, firstChar, lineNum[0]));
                        content = content.substring(1);
                    }
                }
            }

            if (content.length() > 0) {
                if (content.charAt(0) == '\n') {
                    if (content.length() > 1) {
                        lineNum[0]++;
                        content = content.substring(1);
                        continue;
                    } else if (content.length() == 1) {
                        lineNum[0]++;
                        content = "";
                        continue;
                    }
                }

                if (content.charAt(0) == '\t') {
                    if (content.length() > 1) {
                        content = content.substring(1);
                        continue;
                    } else if (content.length() == 1) {
                        content = "";
                        continue;
                    }
                }

                if (content.charAt(0) == ' ') {
                    if (content.length() > 1) {
                        content = content.substring(1);
                        continue;
                    } else if (content.length() == 1) {
                        content = "";
                    }
                }
            }
        }

        tokenize = signConverter(tokenize);
        return tokenize;
    }

}
