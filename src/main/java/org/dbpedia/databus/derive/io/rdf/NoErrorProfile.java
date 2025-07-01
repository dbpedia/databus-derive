package org.dbpedia.databus.derive.io.rdf;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.irix.IRIx;
import org.apache.jena.riot.system.*;
import org.apache.jena.riot.tokens.Token;
import org.apache.jena.sparql.core.Quad;

class NoErrorProfile implements ParserProfile {
    private final ParserProfile base;

    NoErrorProfile(ParserProfile base) {
        this.base = base;
    }

    @Override
    public String resolveIRI(String uriStr, long line, long col) {
        return base.resolveIRI(uriStr, line, col);
    }

    @Override
    public void setBaseIRI(String s) {
        base.setBaseIRI(s);
    }


    @Override
    public Triple createTriple(Node subject, Node predicate, Node object, long line, long col) {
        return base.createTriple(subject, predicate, object, line, col);
    }

    @Override
    public Quad createQuad(Node graph, Node subject, Node predicate, Node object, long line, long col) {
        return base.createQuad(graph, subject, predicate, object, line, col);
    }

    @Override
    public Node createURI(String uriStr, long line, long col) {
        return base.createURI(uriStr, line, col);
    }

    @Override
    public Node createURI(IRIx irIx, long l, long l1) {
        //TODO
        return null;
    }

    @Override
    public Node createTypedLiteral(String lexical, RDFDatatype datatype, long line, long col) {
        return base.createTypedLiteral(lexical, datatype, line, col);
    }

    @Override
    public Node createLangLiteral(String lexical, String langTag, long line, long col) {
        return base.createLangLiteral(lexical, langTag, line, col);
    }

    @Override
    public Node createLangDirLiteral(String s, String s1, String s2, long l, long l1) {
        //TODO
        return null;
    }

    @Override
    public Node createStringLiteral(String lexical, long line, long col) {
        return base.createStringLiteral(lexical, line, col);
    }

    @Override
    public Node createBlankNode(Node scope, String label, long line, long col) {
        return base.createBlankNode(scope, label, line, col);
    }

    @Override
    public Node createBlankNode(Node scope, long line, long col) {
        return base.createBlankNode(scope, line, col);
    }

    @Override
    public Node createTripleTerm(Node node, Node node1, Node node2, long l, long l1) {
        //TODO
        return null;
    }

    @Override
    public Node createTripleNode(Node node, Node node1, Node node2, long l, long l1) {
        return null;
    }

    @Override
    public Node createTripleTerm(Triple triple, long l, long l1) {
        //TODO
        return null;
    }

    @Override
    public Node createTripleNode(Triple triple, long l, long l1) {
        return null;
    }

    @Override
    public Node createGraphNode(Graph graph, long l, long l1) {
        return null;
    }

    @Override
    public Node createNodeFromToken(Node scope, Token token, long line, long col) {
        return base.createNodeFromToken(scope, token, line, col);
    }

    @Override
    public Node create(Node currentGraph, Token token) {
        return base.create(currentGraph, token);
    }

    @Override
    public boolean isStrictMode() {
        return false;
    }

    @Override
    public String getBaseURI() {
        return "";
    }

    @Override
    public PrefixMap getPrefixMap() {
        return base.getPrefixMap();
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return null;
    }

    @Override
    public FactoryRDF getFactorRDF() {
        return base.getFactorRDF();
    }
}
