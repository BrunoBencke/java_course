package cerne.externallookup;

/** A single method: in the tests, a lambda serves as the test double. */
public interface CpfLookup {

    CpfStatus lookUp(String cpf);
}
