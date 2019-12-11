/*
 * generated by Xtext 2.18.0
 */
package org.abs_models.xtext.validation;

import org.abs_models.xtext.abs.*;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.ComposedChecks;

/**
 * This class collects all classes implementing validation rules.
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
// Include other validators: see https://stackoverflow.com/a/17317417
@ComposedChecks(validators = { FullAbsValidator.class, CoreAbsValidator.class })
public class AbsValidator extends AbstractAbsValidator {
}