package dev.shelfie.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/** Shelfie's layering, checked on the compiled classes: graph -> service -> store, never backwards or around. */
@AnalyzeClasses(packages = "dev.shelfie.app", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
        .layer("Graph").definedBy("..graph..")
        .layer("Service").definedBy("..service..")
        .layer("Store").definedBy("..store..")
        .whereLayer("Graph").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Graph")
        .whereLayer("Store").mayOnlyBeAccessedByLayers("Service", "Graph");

    /** Controllers may read the store's records, but only services talk to repositories. */
    @ArchTest
    static final ArchRule controllersNeverTouchRepositories = noClasses().that().resideInAPackage("..graph..")
        .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
        .because("controllers translate GraphQL; the rules (and the data access) live in services");

    @ArchTest
    static final ArchRule noFieldInjection = noFields().should().beAnnotatedWith(Autowired.class)
        .because("constructor injection keeps classes testable without Spring (lab 13.1)");

    @ArchTest
    static final ArchRule servicesKnowNoGraphQL = noClasses().that().resideInAPackage("..service..")
        .should().dependOnClassesThat().resideInAnyPackage("graphql..", "org.springframework.graphql..")
        .because("a service is reused by REST, jobs and tests; GraphQL types belong to the graph layer");
}
