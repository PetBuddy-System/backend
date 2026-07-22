package com.petbuddy.petbuddystore.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class SystemPrompt {
    String prompt;

    public SystemPrompt() throws IOException {
        ClassPathResource resource = new ClassPathResource("prompts/system-prompt.txt");
        this.prompt = resource.getContentAsString(StandardCharsets.UTF_8);
    }

}
