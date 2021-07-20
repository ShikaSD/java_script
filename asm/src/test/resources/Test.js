function myFunction(param1, param2) {
    var hello = "1";
    print(hello)
    print(param2)
    anotherFunction(param1)
    anotherFunction({
        key: hello,
        secondKey: {
            nestedKey: "nestedValue"
        }
    })
}

function anotherFunction(param) {
    print(param)
}

var element = "5"
myFunction(3.0, "2")
print("4")
print(element)
