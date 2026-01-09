package com.appalanche.backend.applications.config;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HtmlSanitizationConfig {

    // TODO: Currently unused, sanitize all other user input.
    @Bean
    public PolicyFactory sanitizerPolicy() {
        return new HtmlPolicyBuilder()
                .allowCommonBlockElements()
                .allowCommonInlineFormattingElements()
                .allowElements("table", "tr", "td", "th", "tbody", "thead")
                .allowElements("a")
                .allowUrlProtocols("https")
                .allowAttributes("href").onElements("a")
                .requireRelNofollowOnLinks()
                .allowAttributes("target").onElements("a")
                .toFactory();
    }
}
