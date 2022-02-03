package de.trundicho.timeclockstamper.core;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.Architectures;

public class ArchitectureTest {

    JavaClasses classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                                                 .importPackages("de.trundicho.timeclockstamper.core");

    @Test
    public void testOnion() {
        Architectures.onionArchitecture()
                     .domainModels("..domain..")
                     .domainServices("..service..")
                     .applicationServices("..app..")
                     .adapter("api", "..adapters.api..")
                     .adapter("persistence", "..adapters.persistence..")
                     .check(classes);
    }
}
