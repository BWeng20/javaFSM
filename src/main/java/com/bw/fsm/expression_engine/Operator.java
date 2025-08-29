package com.bw.fsm.expression_engine;

public enum Operator {
    Multiply,
    Divide,
    Plus,
    Minus,
    Less,
    LessEqual,
    Greater,
    GreaterEqual,
    Assign,
    AssignUndefined,
    Equal,
    NotEqual,
    And,
    Or,

    /**
     * C-like modulus (mathematically the remainder) function.
     */
    Modulus,
    Not
}
