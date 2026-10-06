package com.pagbank.userregistration;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regras de dependência entre slices (PRD seção 6.3 / TASKS T1.6).
 *
 * <p>Executa sobre {@code src/main} apenas; testes não são analisados.
 */
class ArchitectureTest {

	private static final String ROOT = "com.pagbank.userregistration";

	private static JavaClasses classes;

	@BeforeAll
	static void importClasses() {
		classes = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages(ROOT);
	}

	@Test
	void userSliceShouldNotAccessViaCepInternals() {
		ArchRule rule = noClasses()
				.that().resideInAPackage(ROOT + ".user..")
				.should().dependOnClassesThat().resideInAPackage(ROOT + ".addresslookup.viacep..");
		rule.check(classes);
	}

	@Test
	void domainShouldBeFreeOfFrameworkDependencies() {
		ArchRule rule = noClasses()
				.that().resideInAPackage(ROOT + ".user.domain..")
				.should().dependOnClassesThat().resideInAnyPackage(
						"org.springframework.web..",
						"jakarta.persistence..",
						ROOT + ".shared..",
						ROOT + ".addresslookup.viacep..");
		rule.check(classes);
	}

	@Test
	void sharedShouldNotDependOnUserOrAddressLookup() {
		ArchRule rule = noClasses()
				.that().resideInAPackage(ROOT + ".shared..")
				.and().resideOutsideOfPackage(ROOT + ".shared.persistence..")
				.and().resideOutsideOfPackage(ROOT + ".shared.web..")
				.should().dependOnClassesThat().resideInAnyPackage(ROOT + ".user..", ROOT + ".addresslookup..");
		rule.check(classes);
	}

	@Test
	void noLegacyLayeredPackagesAtRoot() {
		ArchRule rule = noClasses()
				.should().resideInAnyPackage(ROOT + ".controller", ROOT + ".service", ROOT + ".repository");
		rule.check(classes);
	}

	@Test
	void everyPackageWithClassesHasNullMarkedPackageInfo() {
		classes.stream()
				.map(clazz -> clazz.getPackageName())
				.distinct()
				.filter(pkg -> pkg.startsWith(ROOT))
				.forEach(pkg -> {
					boolean hasNullMarkedPackageInfo = classes.stream()
							.anyMatch(clazz -> clazz.getFullName().equals(pkg + ".package-info")
									&& clazz.isAnnotatedWith(org.jspecify.annotations.NullMarked.class));
					if (!hasNullMarkedPackageInfo) {
						throw new AssertionError(
								"Pacote '%s' não possui package-info.java anotado com @NullMarked".formatted(pkg));
					}
				});
	}
}
