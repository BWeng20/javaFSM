package com.bw.fsm.datamodel.expression_engine;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.ExpressionException;
import com.bw.fsm.expression_engine.ExpressionParser;
import com.bw.fsm.tracer.DefaultTracer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RFsmExpressionDatamodelTest {

    GlobalData create_global_data() {
        return new GlobalData(new DefaultTracer());
    }

    @Test
    public void can_assign_members() throws ExpressionException {
        var ec = new RFsmExpressionDatamodel(create_global_data());
        Map<String, Data> data_members = new HashMap<>();
        data_members.put("_b", Data.Null.NULL);
        var gd = ec.global_data;
        gd.data.put("a", new Data.Map(data_members));

        var rs = ExpressionParser.execute("a._b = 2", gd);

        System.out.println("Result " + rs);
        assertEquals( new Data.Integer(2), rs);
    }

    @Test
    public void can_assign_variable() throws ExpressionException {
        var ec = new RFsmExpressionDatamodel(create_global_data());
        var rs = ExpressionParser.execute("a = 2", ec.global_data);
        System.out.println("Result " + rs);
    }

    @Test
    public void arrays_work() throws ExpressionException {
        var ec = new RFsmExpressionDatamodel(create_global_data());
        var context = ec.global_data;

        ExpressionParser.execute("v1 ?= [1,2,4, 'abc', ['a', 'b', 'c']]", context);

        var rs = ExpressionParser.execute("v1[1]", context);
        assertEquals(new Data.Integer(2), rs);

        // Cascaded []
        rs = ExpressionParser.execute("v1[v1[1]]", context);
        assertEquals(new Data.Integer(4), rs);

        // Use sub-expression inside []
        rs = ExpressionParser.execute("v1[1+2]", context);
        assertEquals(new Data.String("abc"), rs);

        // Use [] outside []
        rs = ExpressionParser.execute("v1[4][1]", context);
        assertEquals(new Data.String("b"), rs);

        List<Data> abc_array = List.of(
                new Data.String("a"),
                new Data.String("b"),
                new Data.String("c"));
        // Add an element (as standalone element)
        rs = ExpressionParser.execute("['a','b'] + 'c'", context);
        assertEquals(new Data.Array(abc_array), rs);

        // Add an element (as element inside an array)
        rs = ExpressionParser.execute("['a','b'] + ['c']", context);
        assertEquals(new Data.Array(abc_array), rs);

        // as part of some compare
        rs = ExpressionParser.execute("['a']+['b']+'c' == ['a','b'] + ['c']", context);
        assertEquals(Data.Boolean.TRUE, rs);

        // Test if the missing char is detected
        rs = ExpressionParser.execute("['a'] + ['b'] == ['a','b'] + ['c']", context);
        assertEquals(Data.Boolean.FALSE, rs);
    }

    @Test
    public void maps_work() throws ExpressionException {
        var ec = new RFsmExpressionDatamodel(create_global_data());

        var context = ec.global_data;

        ExpressionParser.execute("v1 ?= {'m1':'abc'}", context);
        ExpressionParser.execute("v2 ?= {'m2': 123}", context);

        ExpressionParser.execute("v3 ?= {'m2': 123, 'm1': 'abc'}", context);

        var rs = ExpressionParser.execute("v1.m1", context);
        assertEquals(new Data.String("abc"), rs);

        rs = ExpressionParser.execute("v1 + v2 == v3", context);
        assertEquals(Data.Boolean.TRUE, rs);

        // Assign a new value to field
        rs = ExpressionParser.execute("v3.m1 = 10", context);
        assertEquals(new Data.Integer(10), rs);

        // Now the compare shall return false
        rs = ExpressionParser.execute("v1 + v2 == v3", context);
        assertEquals(Data.Boolean.FALSE, rs);

        // Compare with constants on both sides (also testing an empty map).
        rs = ExpressionParser.execute(
                "{} + {'b':'abc'} + {'a':123}== {'a':123, 'b':'abc'}",
                context);
        assertEquals(Data.Boolean.TRUE, rs);

        // Compare with Empty on both sides
        rs = ExpressionParser.execute("{} == {} ", context);
        assertEquals( Data.Boolean.TRUE, rs);

        // Compare with Empty on one side (shall fail)
        rs = ExpressionParser.execute("{} == {'a':1} ", context);
        assertEquals(Data.Boolean.FALSE, rs);

        // Check that compare fails for additional elements
        rs = ExpressionParser.execute("{'a':1} == {'a':1, 'b':1} ", context);
        assertEquals(Data.Boolean.FALSE, rs);
        rs = ExpressionParser.execute("{'a':1} == {'b':1,'a':1} ", context);
        assertEquals(Data.Boolean.FALSE, rs);

        // Check that identical fields are overwritten by merge
        rs = ExpressionParser.execute("{'a':1} == {'a':null} + {'a':1} ", context);
        assertEquals(Data.Boolean.TRUE, rs);
    }

    @Test
    public void operators_work() throws ExpressionException {

        var ec = new RFsmExpressionDatamodel(create_global_data());
        var context = ec.global_data;

        var rs = ExpressionParser.execute("2 + 1", context);
        System.out.println("Result " + rs);
        assertEquals(new Data.Integer(3), rs );

        rs = ExpressionParser.execute("true | false", context);
        System.out.println("Result " + rs);
        assertEquals(Data.Boolean.TRUE, rs);

        rs = ExpressionParser.execute("true & false", context);
        System.out.println("Result " + rs);
        assertEquals(Data.Boolean.FALSE, rs);

        rs = ExpressionParser.execute("true & !false", context);
        System.out.println("Result " + rs);
        assertEquals(Data.Boolean.TRUE, rs);

        rs = ExpressionParser.execute("!!true & !false", context);
        System.out.println("Result " + rs);
        assertEquals(Data.Boolean.TRUE, rs);

        rs = ExpressionParser.execute("1.0e1 <= 11", context);
        System.out.println("Result " + rs);
        assertEquals(Data.Boolean.TRUE, rs);
    }

    @Test
    public void sequence_work() throws ExpressionException {
        var ec = new RFsmExpressionDatamodel(create_global_data());
        var rs = ExpressionParser.execute("1+1;2+2;3*3", ec.global_data);
        System.out.println("Result " + rs);
        assertEquals(new Data.Integer(9), rs);
    }
}
