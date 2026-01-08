// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "n8nMobileManager",
    platforms: [
        .iOS(.v17),
        .macOS(.v14)
    ],
    products: [
        .library(
            name: "n8nMobileManager",
            targets: ["n8nMobileManager"]),
    ],
    dependencies: [],
    targets: [
        .target(
            name: "n8nMobileManager",
            dependencies: [],
            path: "n8nMobileManager"
        )
    ]
)
