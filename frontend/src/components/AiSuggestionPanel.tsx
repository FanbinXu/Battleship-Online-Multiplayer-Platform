import React from 'react';
import './AiSuggestionPanel.css';

interface Suggestion {
  type: string;
  confidence: number;
  detail: {
    target: { r: number; c: number };
  };
}

interface AiSuggestionPanelProps {
  suggestion: Suggestion | null;
  onRequest: () => void;
  onApply: () => void;
  disabled: boolean;
}

// Custom comparison function for props comparison
const arePropsEqual = (prevProps: AiSuggestionPanelProps, nextProps: AiSuggestionPanelProps): boolean => {
  // Compare primitive values
  if (prevProps.disabled !== nextProps.disabled) return false;
  
  // Compare function references (should be stable with useCallback)
  if (prevProps.onRequest !== nextProps.onRequest) return false;
  if (prevProps.onApply !== nextProps.onApply) return false;
  
  // Deep compare suggestion
  if (!prevProps.suggestion && !nextProps.suggestion) return true;
  if (!prevProps.suggestion || !nextProps.suggestion) return false;
  
  return (
    prevProps.suggestion.type === nextProps.suggestion.type &&
    prevProps.suggestion.confidence === nextProps.suggestion.confidence &&
    prevProps.suggestion.detail.target.r === nextProps.suggestion.detail.target.r &&
    prevProps.suggestion.detail.target.c === nextProps.suggestion.detail.target.c
  );
};

const AiSuggestionPanel: React.FC<AiSuggestionPanelProps> = ({
  suggestion,
  onRequest,
  onApply,
  disabled,
}) => {
  return (
    <div className="ai-panel">
      <h3>🤖 AI Assistant</h3>
      
      {!suggestion ? (
        <div className="ai-request">
          <p>Request an AI-powered attack suggestion</p>
          <button
            onClick={onRequest}
            className="btn-primary"
            disabled={disabled}
          >
            Get AI Suggestion
          </button>
        </div>
      ) : (
        <div className="ai-suggestion">
          <div className="suggestion-details">
            <p className="suggestion-target">
              Target: <strong>Row {suggestion.detail.target.r}, Column {suggestion.detail.target.c}</strong>
            </p>
            <p className="suggestion-confidence">
              Confidence: {(suggestion.confidence * 100).toFixed(0)}%
            </p>
          </div>
          
          <div className="suggestion-actions">
            <button
              onClick={onApply}
              className="btn-primary"
              disabled={disabled}
            >
              Apply Suggestion
            </button>
            <button
              onClick={onRequest}
              className="btn-secondary"
              disabled={disabled}
            >
              Get New Suggestion
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

// Wrap with React.memo for performance optimization
export default React.memo(AiSuggestionPanel, arePropsEqual);



