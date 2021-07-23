function name(param1, param2) {
    var hello = "value";
    var test = {
        key: hello,
        secondKey: {
            nestedKey: "nestedValue"
        }
    };
    hello = test = "result";
    print(hello);
}

name(0.6, "", "", "");
