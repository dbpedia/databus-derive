package org.dbpedia.databus.derive.io.rdf;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.sansa_stack.rdf.common.io.riot.tokens.TokenizerTextForgiving;
import org.apache.jena.atlas.iterator.PeekIterator;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.lang.LangNTuple;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.tokens.Token;
import org.apache.jena.riot.tokens.TokenType;
import org.apache.jena.riot.tokens.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * N-Triples.
 *
 * @see <a href="http://www.w3.org/TR/n-triples/">http://www.w3.org/TR/n-triples/</a>
 */
public final class LangNTriplesSkipBad implements Iterator<TripleWrapper>
{
    private static Logger messageLog = LoggerFactory.getLogger(LangNTriplesSkipBad.class) ;
    private RiotException bad = null;

    @Override
    public boolean hasNext() {
        if (base == null) { return false; }
        try {
            while (base.peek() == null) {
                base.next();
            }
        } catch (NoSuchElementException e) {
            return false;
        }
        return base.hasNext();
    }

    @Override
    public TripleWrapper next() {
        if (bad != null) { throw bad; }
        while ( base.peek() == null ) {  base.next(); }
        return base.next();
    }

    @Override
    public void remove() {
        base.remove();
    }

    static class Wrapper extends LangNTuple<TripleWrapper> {
        /** shadow parent errorHandler */
        private ErrorHandler errorHandler;
        Wrapper(Tokenizer tokens, ParserProfile profile, StreamRDF dest) {
            super(tokens, new NoErrorProfile(profile), dest) ;
            this.errorHandler = profile.getErrorHandler();
        }

        /** Method to parse the whole stream of triples, sending each to the sink */
        @Override
        protected final void runParser() {
            while (hasNext()) {
                Triple x = parseOne();
                if ( x != null )
                    dest.triple(x);
            }
        }

        @Override
        protected final TripleWrapper parseOne() {
            Triple triple = null;
            boolean needSkip = false;
            try {
                Token sToken = nextToken();
                if (sToken.isEOF()) exception(sToken, "Premature end of file: %s", sToken);
                needSkip = true; checkIRIOrBNode(sToken); needSkip = false;

                Token pToken = nextToken();
                if (pToken.isEOF()) exception(pToken, "Premature end of file: %s", pToken);
                needSkip = true; checkIRI(pToken); needSkip = false;

                Token oToken = nextToken();
                if (oToken.isEOF()) exception(oToken, "Premature end of file: %s", oToken);
                needSkip = true; checkRDFTerm("object", oToken); needSkip = false;

                Token x = nextToken();
                if (x.getType() != TokenType.DOT) exception(x, "Triple not terminated by DOT: %s", x);


                triple = profile.createTriple(
                        tokenAsNode(sToken), tokenAsNode(pToken), tokenAsNode(oToken), sToken.getLine(), sToken.getColumn());
            } catch (RiotParseException e) {
                if (needSkip) {
                    ((TokenizerTextForgiving)tokens).skipLine();
                    nextToken();
                } else { /** this is handled by {@link TokenizerTextForgiving} **/ }
            } catch (NullPointerException e2) {
                errorHandler.warning(e2.getMessage(), currLine, currCol);
            }
            if(triple != null) {
                TripleWrapper tw = new TripleWrapper(triple.getSubject(),triple.getPredicate(),triple.getObject());
                tw.setRow(currLine);
                return tw;
            } else {
                return null;
            }
        }

        @Override
        protected final Node tokenAsNode(Token token) {
            return profile.create(null, token) ;
        }

        @Override
        public Lang getLang()   { return RDFLanguages.NTRIPLES ; }

    }

    private PeekIterator<TripleWrapper> base = null;

    public LangNTriplesSkipBad(TokenizerTextForgiving tokens, ParserProfile profile, StreamRDF dest) {
        try { base = new PeekIterator<>(new LangNTriplesSkipBad.Wrapper(tokens, profile, dest)); }
        catch (RiotException e) {
            bad = e;
            profile.getErrorHandler().warning(e.getMessage(), -1, -1);
        }
    }
}
