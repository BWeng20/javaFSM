package com.bw.fsm.expression_engine;

public enum TokenType {
    /// Some constant number. Integer or float.
    Number,
    /// An identifier
    Identifier,
    /// Some constant string expression
    TString,
    /// A constant boolean expression.
    Boolean,
    /// Some operator
    Operator,
    /// Some bracket
    Bracket,
    /// A - none whitespace, none bracket - separator
    Separator,
    /// The expression separator to join multiple expressions.
    ExpressionSeparator,
    /// a Null value
    Null,
    /// Indicates a lexer error.
    Error,
    /// Indicates the end of the expression.
    EOE,
}
