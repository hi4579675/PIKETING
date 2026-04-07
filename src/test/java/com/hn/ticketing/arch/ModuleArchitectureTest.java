package com.hn.ticketing.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;


@AnalyzeClasses(packages = "com.hn.ticketing")
class ModuleArchitectureTest {

    @ArchTest
    static final ArchRule no_cycles =
            SlicesRuleDefinition.slices()
                    .matching("..ticketing.(*)..")
                    .should().beFreeOfCycles();
}

