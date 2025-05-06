package com.mahaoyang.maaiagent.rag;

import dev.langchain4j.data.document.DocumentLoader;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *@Author: 叶上初阳
 *
 */
// 添加手动日志定义
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class LoveAppDocumentLoader {
    
    // 手动创建日志记录器
    private static final Logger log = LoggerFactory.getLogger(LoveAppDocumentLoader.class);
    
    private final ResourcePatternResolver resourcePatternResolver;

    public LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载多篇 Markdown 文档
     * @return
     */
    public List<Document> loadDocuments() {
        List<Document> allDocuments = new ArrayList<>();
        // 加载多篇 Markdown 文档
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", fileName)
                        .build();
                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(markdownDocumentReader.get());
            }
        } catch (Exception e) {
            log.error("Error loading documents: {}", e.getMessage()); // 现在可以正常使用log
        }
        return allDocuments;
    }

}
