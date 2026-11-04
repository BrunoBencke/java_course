package cerne.externallookup;

/** A single method: in the tests, a lambda works as a test double. */
public interface CpfLookup {

    CpfStatus lookUp(String cpf);
}
