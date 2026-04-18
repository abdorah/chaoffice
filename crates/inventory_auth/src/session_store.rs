use std::path::PathBuf;
use std::sync::Mutex;

use inventory_security::AuthError;

/// Trait for frontend-specific session token persistence.
pub trait SessionStore: Send + Sync {
    fn save_token(&self, token: &str) -> Result<(), AuthError>;
    fn load_token(&self) -> Result<Option<String>, AuthError>;
    fn clear_token(&self) -> Result<(), AuthError>;
}

/// Desktop (Slint): in-memory store, lost on app close.
pub struct InMemorySessionStore {
    token: Mutex<Option<String>>,
}

impl InMemorySessionStore {
    pub fn new() -> Self {
        Self {
            token: Mutex::new(None),
        }
    }
}

impl Default for InMemorySessionStore {
    fn default() -> Self {
        Self::new()
    }
}

impl SessionStore for InMemorySessionStore {
    fn save_token(&self, token: &str) -> Result<(), AuthError> {
        let mut guard = self.token.lock().map_err(|e| {
            AuthError::Internal(format!("lock poisoned: {e}"))
        })?;
        *guard = Some(token.to_string());
        Ok(())
    }

    fn load_token(&self) -> Result<Option<String>, AuthError> {
        let guard = self.token.lock().map_err(|e| {
            AuthError::Internal(format!("lock poisoned: {e}"))
        })?;
        Ok(guard.clone())
    }

    fn clear_token(&self) -> Result<(), AuthError> {
        let mut guard = self.token.lock().map_err(|e| {
            AuthError::Internal(format!("lock poisoned: {e}"))
        })?;
        *guard = None;
        Ok(())
    }
}

/// CLI: file-based session store that persists the token to a file path.
pub struct FileSessionStore {
    path: PathBuf,
}

impl FileSessionStore {
    pub fn new(path: PathBuf) -> Self {
        Self { path }
    }
}

impl SessionStore for FileSessionStore {
    fn save_token(&self, token: &str) -> Result<(), AuthError> {
        if let Some(parent) = self.path.parent() {
            std::fs::create_dir_all(parent).map_err(|e| {
                AuthError::Internal(format!("failed to create session dir: {e}"))
            })?;
        }
        std::fs::write(&self.path, token).map_err(|e| {
            AuthError::Internal(format!("failed to write session file: {e}"))
        })
    }

    fn load_token(&self) -> Result<Option<String>, AuthError> {
        match std::fs::read_to_string(&self.path) {
            Ok(contents) => {
                let trimmed = contents.trim().to_string();
                if trimmed.is_empty() {
                    Ok(None)
                } else {
                    Ok(Some(trimmed))
                }
            }
            Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(None),
            Err(e) => Err(AuthError::Internal(format!(
                "failed to read session file: {e}"
            ))),
        }
    }

    fn clear_token(&self) -> Result<(), AuthError> {
        match std::fs::remove_file(&self.path) {
            Ok(()) => Ok(()),
            Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(()),
            Err(e) => Err(AuthError::Internal(format!(
                "failed to remove session file: {e}"
            ))),
        }
    }
}
