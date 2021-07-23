function name(param1, param2) {
    var hello = "value";
    var test = {
        key: hello,
        secondKey: {
            nestedKey: "nestedValue"
        }
    };
    test = hello = test;
    print(hello);
}

name(0.6, "");
