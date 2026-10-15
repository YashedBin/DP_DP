package model;

public class Question {
    private Long id;
    private Long examId;
    private String type;
    private String text;
    private int marks;
    private String options;

    public Question() {}

    public Question(Long id, Long examId, String type, String text, int marks, String options) {
        this.id = id;
        this.examId = examId;
        this.type = type;
        this.text = text;
        this.marks = marks;
        this.options = options;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getMarks() { return marks; }
    public void setMarks(int marks) { this.marks = marks; }

    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
}
