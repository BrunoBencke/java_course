package cerne.externallookup;

import cerne.members.Cpf;

/** A single method: in the tests, a lambda works as a test double without a mocking library. */
@FunctionalInterface
public interface CpfLookup {

    CpfStatus lookUp(Cpf cpf);
}
