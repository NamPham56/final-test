import type { MediaChanges,MediaInfo } from './common'
export interface Lesson extends LessonForm { id:number;media:MediaInfo[];createdAt:string;updatedAt:string }
export interface LessonForm extends MediaChanges { courseId:number;lessonCode?:string;title:string;description?:string;durationSeconds?:number;lessonOrder:number }
