import { WORKFLOW_STEPS, getStepIndex } from '../utils/constants';
import type { StatutDA } from '../types';

interface Props { statut: StatutDA; }

export default function WorkflowStepper({ statut }: Props) {
  const currentIdx = getStepIndex(statut);
  const rejected = statut === 'REJETEE';

  return (
    <div className="w-full overflow-x-auto py-2">
      <div className="flex items-center min-w-max gap-0">
        {WORKFLOW_STEPS.map((step, i) => {
          const done    = !rejected && i < currentIdx;
          const active  = !rejected && i === currentIdx;
          const pending = rejected || i > currentIdx;

          return (
            <div key={step.statut} className="flex items-center">
              {/* Step bubble */}
              <div className="flex flex-col items-center gap-1">
                <div className={`
                  w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold transition-all
                  ${done   ? 'bg-green-500 text-white shadow-md shadow-green-200' : ''}
                  ${active ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-200 scale-110 ring-4 ring-indigo-100' : ''}
                  ${pending ? 'bg-gray-100 text-gray-400' : ''}
                  ${rejected && i === currentIdx ? 'bg-red-500 text-white' : ''}
                `}>
                  {done ? '✓' : step.icon}
                </div>
                <span className={`text-[10px] font-medium whitespace-nowrap ${active ? 'text-indigo-700' : done ? 'text-green-600' : 'text-gray-400'}`}>
                  {step.label}
                </span>
              </div>
              {/* Connector line */}
              {i < WORKFLOW_STEPS.length - 1 && (
                <div className={`h-0.5 w-8 mx-1 mt-[-10px] rounded-full transition-colors ${done ? 'bg-green-400' : 'bg-gray-200'}`} />
              )}
            </div>
          );
        })}
        {/* Rejected marker */}
        {rejected && (
          <>
            <div className="h-0.5 w-8 mx-1 mt-[-10px] rounded-full bg-red-300" />
            <div className="flex flex-col items-center gap-1">
              <div className="w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold bg-red-500 text-white shadow-md shadow-red-200">
                ✕
              </div>
              <span className="text-[10px] font-medium text-red-600">Rejetée</span>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
