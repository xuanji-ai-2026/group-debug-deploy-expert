//
//  BxAppUITests.swift
//  BxAppUITests
//
//  北极星AI商机获客系统 - iOS客户端
//  UI测试
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import XCTest

final class BxAppUITests: XCTestCase {

    override func setUpWithError() throws {
        // Setup code before each test
        continueAfterFailure = false
    }

    override func tearDownWithError() throws {
        // Teardown code after each test
    }

    // MARK: - Launch Tests
    
    func testAppLaunch() throws {
        let app = XCUIApplication()
        app.launch()
        
        // Verify app launches successfully
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 5))
    }
    
    // MARK: - Login Flow Tests
    
    func testLoginViewExists() throws {
        let app = XCUIApplication()
        app.launch()
        
        // Check for login button
        let loginButton = app.buttons["登录"]
        XCTAssertTrue(loginButton.waitForExistence(timeout: 5))
    }
    
    // MARK: - Navigation Tests
    
    func testTabNavigation() throws {
        let app = XCUIApplication()
        app.launch()
        
        // Wait for main view to load
        sleep(2)
        
        // Navigate through tabs if available
        let tabBar = app.tabBars.firstMatch
        if tabBar.exists {
            // Test tab navigation
        }
    }
    
    // MARK: - Performance Tests
    
    func testAppLaunchPerformance() throws {
        if #available(iOS 13.0, *) {
            measure(metrics: [XCTApplicationLaunchMetric()]) {
                XCUIApplication().launch()
            }
        }
    }
}
