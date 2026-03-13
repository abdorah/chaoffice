// Sweet Lab Desktop — Slint UI shell
// Calls sweet-lab-core directly (no FFI needed, same Rust crate)

slint::include_modules!();

fn main() {
    let app = AppWindow::new().unwrap();

    // -- Login callback --
    // Req 1.1: Authenticate and create session with corresponding role
    // Req 1.2: Generic error message — does not reveal which field is wrong
    // Req 1.3: Role-based redirect after login
    let app_weak = app.as_weak();
    app.on_login(move |username, password| {
        let app = app_weak.unwrap();
        let username = username.to_string();
        let _password = password.to_string();

        // Show loading state
        app.set_login_loading(true);
        app.set_login_error("".into());

        // TODO: Wire to SweetLabCore.login() once core is fully initialized
        // let rt = tokio::runtime::Runtime::new().unwrap();
        // let core = SweetLabCore::new("sweet_lab.db").await.unwrap();
        // match rt.block_on(core.login(&username, &password)) {
        //     Ok(session) => {
        //         match session.role {
        //             UserRole::Admin => app.set_current_view(ActiveView::AdminDashboard),
        //             UserRole::Chef => app.set_current_view(ActiveView::ChefProduction),
        //             UserRole::Representative => app.set_current_view(ActiveView::RepresentativeSales),
        //         }
        //         app.set_current_role(session.role.into());
        //     }
        //     Err(_) => {
        //         // Req 1.2: Generic error — never reveal which field is wrong
        //         app.set_login_error("بيانات الدخول غير صحيحة".into());
        //     }
        // }

        // Placeholder: route by username prefix for scaffold testing
        if username == "admin" {
            app.set_current_role(UserRole::Admin);
            app.set_current_view(ActiveView::AdminDashboard);
            app.set_login_error("".into());
        } else if username == "chef" {
            app.set_current_role(UserRole::Chef);
            app.set_current_view(ActiveView::ChefProduction);
            app.set_login_error("".into());
        } else if username == "rep" {
            app.set_current_role(UserRole::Representative);
            app.set_current_view(ActiveView::RepresentativeSales);
            app.set_login_error("".into());
        } else {
            // Req 1.2: Generic error — does not reveal which field is incorrect
            app.set_login_error("بيانات الدخول غير صحيحة".into());
        }

        // Clear loading state
        app.set_login_loading(false);
    });

    // -- Logout callback --
    let app_weak = app.as_weak();
    app.on_logout(move || {
        let app = app_weak.unwrap();
        app.set_current_view(ActiveView::Login);
        app.set_current_role(UserRole::None);
        app.set_login_error("".into());
        app.set_login_loading(false);
        app.set_nav_index(0);
    });

    // -- Navigation callback --
    let app_weak = app.as_weak();
    app.on_navigate(move |index| {
        let app = app_weak.unwrap();
        app.set_nav_index(index);

        // TODO: Expand navigation to switch between sub-views per role
        // For now the sidebar buttons update nav_index; sub-view switching
        // will be added in tasks 24.3–24.5
    });

    app.run().unwrap();
}
