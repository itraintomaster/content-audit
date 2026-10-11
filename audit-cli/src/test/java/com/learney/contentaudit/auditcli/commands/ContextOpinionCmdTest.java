package com.learney.contentaudit.auditcli.commands;
import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.batch.ContextOpinion.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class ContextOpinionCmdTest {
    @Test void helpAndMalformedJsonNeverConstructTheService() {
        var c=new CommandLine(new ContextOpinionCmd(() -> { fail("unexpected service"); return null; }));
        c.setOut(new PrintWriter(new StringWriter())); c.setErr(new PrintWriter(new StringWriter()));
        assertEquals(0,c.execute("--help")); assertEquals(2,c.execute("--request-json","bad JSON"));
    }
    @Test void roundTripsScopeAndZeroBudget() {
        var received=new AtomicReference<Request>();
        var c=new CommandLine(new ContextOpinionCmd(() -> r -> { received.set(r); return new State("b",3,"fp",true,null,0,4,List.of(),r.selection(),r.wholeTopic(),List.of()); }));
        var out=new StringWriter(); c.setOut(new PrintWriter(out));
        assertEquals(0,c.execute("--request-json", """
          {"action":"measure","batchId":"b","selection":{"mini-theory":["q1","q2"]},"wholeTopic":true,
          "operationId":"op","contentVersion":3,"fingerprint":"fp","queryCap":0,"reviewer":"human"}
          """));
        assertEquals(0,received.get().queryCap()); assertTrue(received.get().wholeTopic());
        assertEquals(List.of("q1","q2"),received.get().selection().get("mini-theory")); assertTrue(out.toString().contains("\"reusedQueries\":4"));
    }
}
