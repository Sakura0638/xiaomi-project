package com.xiaomiproject.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
// 这个注解很有用，如果API返回了我们未定义的字段，程序不会报错
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmResponse {
    private List<Choice> choices;
}
